package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.ScholarlyWork;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService.ArticleMetadataSource;
import org.ambraproject.rhino.util.Archive;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.plos.crepo.model.RepoObject;
import org.plos.crepo.model.RepoVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class VersionedIngestionService {

  private static final Logger log = LoggerFactory.getLogger(VersionedIngestionService.class);

  private final ArticleCrudServiceImpl parentService;

  VersionedIngestionService(ArticleCrudServiceImpl parentService) {
    this.parentService = Preconditions.checkNotNull(parentService);
  }

  private static long getLastInsertId(Session session) {
    return ((Number) session.createSQLQuery("SELECT LAST_INSERT_ID()").uniqueResult()).longValue();
  }

  Article ingest(Archive archive, OptionalInt revision) throws IOException, XmlContentException {
    String manifestEntry = null;
    for (String entryName : archive.getEntryNames()) {
      if (entryName.equalsIgnoreCase("manifest.xml")) {
        manifestEntry = entryName;
      }
    }
    if (manifestEntry == null) {
      throw new RestClientException("Archive has no manifest file", HttpStatus.BAD_REQUEST);
    }

    ManifestXml manifestXml;
    try (InputStream manifestStream = new BufferedInputStream(archive.openFile(manifestEntry))) {
      manifestXml = new ManifestXml(AmbraService.parseXml(manifestStream));
    }
    validateManifestCompleteness(manifestXml, archive);

    ImmutableList<ManifestXml.Asset> assets = manifestXml.getAssets();
    ManifestXml.Asset manuscriptAsset = findManuscriptAsset(assets);
    ManifestXml.Representation manuscriptRepr = findManuscriptRepr(manuscriptAsset);
    ManifestXml.Representation printableRepr = findPrintableRepr(manuscriptAsset);

    String manuscriptEntry = manuscriptRepr.getFile().getEntry();
    if (!archive.getEntryNames().contains(manifestEntry)) {
      throw new RestClientException("Manifest refers to missing file as main-entry: " + manuscriptEntry, HttpStatus.BAD_REQUEST);
    }

    ArticleXml parsedArticle;
    try (InputStream manuscriptStream = new BufferedInputStream(archive.openFile(manuscriptEntry))) {
      parsedArticle = new ArticleXml(AmbraService.parseXml(manuscriptStream));
    }
    ArticleIdentity articleIdentity = parsedArticle.readDoi();
    if (!manuscriptAsset.getUri().equals(articleIdentity.getKey())) {
      String message = String.format("Article DOI is inconsistent. From manifest: \"%s\" From manuscript: \"%s\"",
          manuscriptAsset.getUri(), articleIdentity.getKey());
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }

    long endeavorId = getEndeavorId(articleIdentity);
    long ingestionEventId = getIngestionEventId(endeavorId, revision.orElseGet(parsedArticle::getRevisionNumber));

    final Article articleMetadata = parsedArticle.build(new Article());
    articleMetadata.setDoi(articleIdentity.getKey());

    ArticlePackage articlePackage = new ArticlePackageBuilder(archive, parsedArticle, manifestXml, manifestEntry,
        manuscriptAsset, manuscriptRepr, printableRepr).build();
    persist(articlePackage, ingestionEventId);
    persistJournal(articleMetadata, ingestionEventId);

    stubAssociativeFields(articleMetadata);
    return articleMetadata;
  }

  private long getEndeavorId(ArticleIdentity articleIdentity) {
    String endeavorDoi = articleIdentity.getIdentifier();
    return parentService.hibernateTemplate.execute(session -> {
      SQLQuery selectQuery = session.createSQLQuery("SELECT endeavorId FROM endeavor WHERE doi = :doi");
      selectQuery.setParameter("doi", endeavorDoi);
      Number endeavorId = (Number) selectQuery.uniqueResult();
      if (endeavorId != null) return endeavorId.longValue();

      SQLQuery insertQuery = session.createSQLQuery("INSERT INTO endeavor (doi) VALUES (:doi)");
      insertQuery.setParameter("doi", endeavorDoi);
      insertQuery.executeUpdate();
      return getLastInsertId(session);
    });
  }

  private long getIngestionEventId(long endeavorId, int revisionNumber) {
    return parentService.hibernateTemplate.execute(session -> {
      SQLQuery blankOtherRevisions = session.createSQLQuery("" +
          "UPDATE ingestionEvent SET revisionNumber = NULL, lastModified = NOW() " +
          "WHERE endeavorId=:endeavorId AND revisionNumber=:revisionNumber");
      blankOtherRevisions.setParameter("endeavorId", endeavorId);
      blankOtherRevisions.setParameter("revisionNumber", revisionNumber);
      blankOtherRevisions.executeUpdate();

      SQLQuery insertEvent = session.createSQLQuery("" +
          "INSERT INTO ingestionEvent (endeavorId, revisionNumber, publicationState, lastModified) " +
          "VALUES (:endeavorId, :revisionNumber, :publicationState, NOW())");
      insertEvent.setParameter("endeavorId", endeavorId);
      insertEvent.setParameter("revisionNumber", revisionNumber);
      insertEvent.setParameter("publicationState", 0); // TODO: Set initial value
      insertEvent.executeUpdate();
      return getLastInsertId(session);
    });
  }

  private void validateManifestCompleteness(ManifestXml manifest, Archive archive) {
    Set<String> archiveEntryNames = archive.getEntryNames();

    Stream<ManifestXml.ManifestFile> manifestFiles = Stream.concat(
        manifest.getAssets().stream()
            .flatMap(asset -> asset.getRepresentations().stream())
            .map(ManifestXml.Representation::getFile),
        manifest.getArchivalFiles().stream());
    Set<String> manifestEntryNames = manifestFiles
        .map(ManifestXml.ManifestFile::getEntry)
        .collect(Collectors.toSet());

    Set<String> missingFromArchive = Sets.difference(manifestEntryNames, archiveEntryNames).immutableCopy();
    Set<String> missingFromManifest = Sets.difference(archiveEntryNames, manifestEntryNames).immutableCopy();
    if (!missingFromArchive.isEmpty() || !missingFromManifest.isEmpty()) {
      String message = "Manifest is not consistent with files in archive."
          + (missingFromArchive.isEmpty() ? "" : (" Files in manifest not included in archive: " + missingFromArchive))
          + (missingFromManifest.isEmpty() ? "" : (" Files in archive not described in manifest: " + missingFromManifest));
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }
  }

  private void persist(ArticlePackage articlePackage, long ingestionEventId) {
    for (ScholarlyWorkInput work : articlePackage.getAllWorks()) {
      persist(work, ingestionEventId);
    }
    persistArchivalFiles(articlePackage, ingestionEventId);
  }

  private long persist(ScholarlyWorkInput work, long ingestionEventId) {
    Map<String, RepoVersion> crepoResults = new LinkedHashMap<>();
    for (Map.Entry<String, RepoObject> entry : work.getObjects().entrySet()) {
      RepoVersion result = parentService.contentRepoService.autoCreateRepoObject(entry.getValue()).getVersion();
      crepoResults.put(entry.getKey(), result);
    }

    return parentService.hibernateTemplate.execute(session -> {
      SQLQuery insertWork = session.createSQLQuery("" +
          "INSERT INTO scholarlyWork (ingestionEventId, doi, scholarlyWorkType) " +
          "  VALUES (:ingestionEventId, :doi, :scholarlyWorkType)");
      insertWork.setParameter("ingestionEventId", ingestionEventId);
      insertWork.setParameter("doi", work.getDoi().getIdentifier());
      insertWork.setParameter("scholarlyWorkType", work.getType());
      insertWork.executeUpdate();
      long scholarlyWorkId = getLastInsertId(session);

      for (Map.Entry<String, RepoVersion> entry : crepoResults.entrySet()) {
        SQLQuery insertFile = session.createSQLQuery("" +
            "INSERT INTO ingestedFile (ingestionEventId, scholarlyWorkId, fileType, crepoKey, crepoUuid) " +
            "  VALUES (:ingestionEventId, :scholarlyWorkId, :fileType, :crepoKey, :crepoUuid)");
        insertFile.setParameter("ingestionEventId", ingestionEventId);
        insertFile.setParameter("scholarlyWorkId", scholarlyWorkId);
        insertFile.setParameter("fileType", entry.getKey());

        RepoVersion repoVersion = entry.getValue();
        insertFile.setParameter("crepoKey", repoVersion.getKey());
        insertFile.setParameter("crepoUuid", repoVersion.getUuid().toString());

        insertFile.executeUpdate();
      }

      return scholarlyWorkId;
    });
  }

  private void persistArchivalFiles(ArticlePackage articlePackage, long ingestionEventId) {
    List<RepoVersion> archivalFiles = articlePackage.getArchivalFiles().stream()
        .map(archivalFile -> parentService.contentRepoService.autoCreateRepoObject(archivalFile).getVersion())
        .collect(Collectors.toList());
    parentService.hibernateTemplate.execute(session -> {
      for (RepoVersion archivalFile : archivalFiles) {
        SQLQuery insertFile = session.createSQLQuery("" +
            "INSERT INTO ingestedFile (ingestionEventId, crepoKey, crepoUuid) " +
            "  VALUES (:ingestionEventId, :crepoKey, :crepoUuid)");
        insertFile.setParameter("ingestionEventId", ingestionEventId);
        insertFile.setParameter("crepoKey", archivalFile.getKey());
        insertFile.setParameter("crepoUuid", archivalFile.getUuid());
        insertFile.executeUpdate();
      }
      return null;
    });
  }

  private long persistJournal(Article article, long ingestionEventId) {
    Journal publicationJournal = parentService.getPublicationJournal(article);
    return parentService.hibernateTemplate.execute(session -> {
      SQLQuery query = session.createSQLQuery("" +
          "INSERT INTO publishedInJournal (ingestionEventId, journalId) " +
          "VALUES (:ingestionEventId, :journalId)");
      query.setParameter("ingestionEventId", ingestionEventId);
      query.setParameter("journalId", publicationJournal.getID());
      query.executeUpdate();
      return getLastInsertId(session);
    });
  }

  private void stubAssociativeFields(Article article) {
    article.setID(-1L);
    article.setAssets(ImmutableList.of());
    article.setRelatedArticles(ImmutableList.of());
    article.setJournals(ImmutableSet.of());
    article.setCategories(ImmutableMap.of());
  }

  private ManifestXml.Asset findManuscriptAsset(List<ManifestXml.Asset> assets) {
    for (ManifestXml.Asset asset : assets) {
      if (asset.getMainEntry().isPresent()) {
        return asset;
      }
    }
    throw new RestClientException("main-entry not found", HttpStatus.BAD_REQUEST);
  }

  private ManifestXml.Representation findPrintableRepr(ManifestXml.Asset manuscriptAsset) {
    Optional<String> mainEntry = manuscriptAsset.getMainEntry();
    Preconditions.checkArgument(mainEntry.isPresent(), "manuscriptAsset must have main-entry");
    for (ManifestXml.Representation representation : manuscriptAsset.getRepresentations()) {
      if (representation.getName().equals("PDF")) {
        return representation;
      }
    }
    throw new RestClientException("main-entry not matched to asset", HttpStatus.BAD_REQUEST);
  }

  private ManifestXml.Representation findManuscriptRepr(ManifestXml.Asset manuscriptAsset) {
    Optional<String> mainEntry = manuscriptAsset.getMainEntry();
    Preconditions.checkArgument(mainEntry.isPresent(), "manuscriptAsset must have main-entry");
    for (ManifestXml.Representation representation : manuscriptAsset.getRepresentations()) {
      if (representation.getFile().getEntry().equals(mainEntry.get())) {
        return representation;
      }
    }
    throw new RestClientException("main-entry not matched to asset", HttpStatus.BAD_REQUEST);
  }


  /**
   * Build a representation of an article's metadata from a persisted collection.
   * <p>
   * The legacy Hibernate model object {@link Article} is used as a data-holder for convenience and compatibility. This
   * method constructs it anew, not by accessing Hibnerate, and populates only a subset of its normal fields.
   *
   * @param id     the ID of the article to serve
   * @param source whether to parse the extracted front matter or the full, original manuscript
   * @return an object containing metadata that could be extracted from the manuscript, with other fields unfilled
   * @deprecated method signature accommodates testing and will be changed
   */
  @Deprecated
  Article getArticleMetadata(ArticleIdentity id, OptionalInt revisionNumber, ArticleMetadataSource source) {
    /*
     * *** Implementation notes ***
     *
     * The method signature accommodates the methods `ArticleCrudController.previewMetadataFromVersionedModel` and
     * `ArticleCrudService.readVersionedMetadata`, which are temporary hacks to expose read-service functionality for
     * testing. This method's signature will probably change when those methods are removed, though we expect to keep
     * most of the business logic.
     *
     * The `source` argument ought to be replaced with something that doesn't expose so much detail. It gives the option
     * to parse the full manuscript (including the body) for validation purposes, which should not be possible in the
     * production implementation. We do currently plan to choose between the 'front' and 'frontAndBack' documents based
     * on whether we need to serve cited articles, but the signature should talk about whether to include citations, not
     * which file to read. In a future API version, we may wish to simplify further by splitting citations into a
     * separate service.
     *
     * TODO: Improve as described above and delete this comment block
     */

    ScholarlyWork work = parentService.getScholarlyWork(id, revisionNumber);

    RepoVersion manuscriptVersion = work.getFile("manuscript").orElseThrow(() -> {
      String message = String.format("Work exists but does not have a manuscript. DOI: %s. Revision: %s",
          work.getDoi(), work.getRevisionNumber().map(Object::toString).orElse("(none)"));
      return new RestClientException(message, HttpStatus.BAD_REQUEST);
    });

    Document document;
    try (InputStream manuscriptStream = parentService.contentRepoService.getRepoObject(manuscriptVersion)) {
      DocumentBuilder documentBuilder = AmbraService.newDocumentBuilder();
      log.debug("In getArticleMetadata source={} documentBuilder.parse() called", source);
      document = documentBuilder.parse(manuscriptStream);
      log.debug("finish");
    } catch (IOException | SAXException e) {
      throw new RuntimeException(e);
    }

    Article article;
    try {
      article = new ArticleXml(document).build(new Article());
    } catch (XmlContentException e) {
      throw new RuntimeException(e);
    }
    return article;
  }
}
