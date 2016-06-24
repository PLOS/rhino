package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleFile;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ArticleVisibility;
import org.ambraproject.rhino.model.Journal;
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
    ArticleIdentifier articleIdentifier = ArticleIdentifier.create(parsedArticle.readDoi().getIdentifier());
    Doi doi = articleIdentifier.getDoi();
    if (!manuscriptAsset.getUri().equals(doi.getUri().toString())) {
      String message = String.format("Article DOI is inconsistent. From manifest: \"%s\" From manuscript: \"%s\"",
          manuscriptAsset.getUri(), doi.getUri());
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }

    long articlePk = persistArticlePk(articleIdentifier);
    long ingestionId = persistIngestion(articlePk);
    persistRevision(articlePk, ingestionId, revision.orElseGet(parsedArticle::getRevisionNumber));

    final Article articleMetadata = parsedArticle.build(new Article());
    articleMetadata.setDoi(doi.getUri().toString());

    ArticlePackage articlePackage = new ArticlePackageBuilder(archive, parsedArticle, manifestXml, manifestEntry,
        manuscriptAsset, manuscriptRepr, printableRepr).build();
    persistItem(articlePackage, ingestionId);
    persistJournal(articleMetadata, ingestionId);

    stubAssociativeFields(articleMetadata);

    ArticleIngestionIdentifier ingestionIdentifier = ArticleIngestionIdentifier.create(
        articleIdentifier.getDoi(), parsedArticle.getRevisionNumber());

    if (isSyndicatableType(articleMetadata.getTypes())) {
//      parentService.syndicationService.createSyndications(ingestionIdentifier);
    }

    return articleMetadata;
  }

  /**
   * @param articleTypes All the article types of the Article for which Syndication objects are being created
   * @return Whether to create a Syndication object for this Article
   */
  private boolean isSyndicatableType(Set<String> articleTypes) {
    String articleTypeDoNotCreateSyndication = "http://rdf.plos.org/RDF/articleType/Issue%20Image";
    return !(articleTypes != null && articleTypes.contains(articleTypeDoNotCreateSyndication));
  }


  /**
   * Get the PK of the "article" row for a DOI if it exists, and insert it if it doesn't.
   *
   * @param articleIdentifier
   */
  private long persistArticlePk(ArticleIdentifier articleIdentifier) {
    String articleDoi = articleIdentifier.getDoiName();
    return parentService.hibernateTemplate.execute(session -> {
      SQLQuery selectQuery = session.createSQLQuery("SELECT articleId FROM article WHERE doi = :doi");
      selectQuery.setParameter("doi", articleDoi);
      Number articlePk = (Number) selectQuery.uniqueResult();
      if (articlePk != null) return articlePk.longValue();

      SQLQuery insertQuery = session.createSQLQuery("INSERT INTO article (doi) VALUES (:doi)");
      insertQuery.setParameter("doi", articleDoi);
      insertQuery.executeUpdate();
      return getLastInsertId(session);
    });
  }

  private static final int FIRST_INGESTION_NUMBER = 1;

  private long persistIngestion(long articlePk) {
    return parentService.hibernateTemplate.execute(session -> {
      SQLQuery findNextIngestionNumber = session.createSQLQuery(
          "SELECT MAX(ingestionNumber) FROM articleIngestion WHERE articleId = :articleId");
      findNextIngestionNumber.setParameter("articleId", articlePk);
      Number maxIngestionNumber = (Number) findNextIngestionNumber.uniqueResult();
      int nextIngestionNumber = (maxIngestionNumber == null) ? FIRST_INGESTION_NUMBER
          : maxIngestionNumber.intValue() + 1;

      SQLQuery insertEvent = session.createSQLQuery("" +
          "INSERT INTO articleIngestion (articleId, ingestionNumber, visibility) " +
          "VALUES (:articleId, :ingestionNumber, :visibility)");
      insertEvent.setParameter("articleId", articlePk);
      insertEvent.setParameter("ingestionNumber", nextIngestionNumber);
      insertEvent.setParameter("visibility", ArticleVisibility.INGESTED.getValue());
      insertEvent.executeUpdate();
      return getLastInsertId(session);
    });
  }

  private void persistRevision(long articleId, long ingestionId, int revisionNumber) {
    parentService.hibernateTemplate.execute(session -> {
      // Find an articleRevision row (if it exists) that has the same revision number and belongs to the same article.
      // Join articleRevision through articleIngestion to get to the "grandparent" article.
      SQLQuery findExistingRevision = session.createSQLQuery("" +
          "SELECT articleRevision.revisionId " +
          "FROM articleRevision " +
          "  INNER JOIN articleIngestion ON articleRevision.ingestionId = articleIngestion.ingestionId " +
          "  INNER JOIN article ON articleIngestion.articleId = article.articleId " +
          "WHERE articleRevision.revisionNumber = :revisionNumber " +
          "  AND article.articleId = :articleId");
      findExistingRevision.setParameter("revisionNumber", revisionNumber);
      findExistingRevision.setParameter("articleId", articleId);
      Number existingRevisionId = (Number) findExistingRevision.uniqueResult();

      if (existingRevisionId != null) {
        SQLQuery updateRevision = session.createSQLQuery("" +
            "UPDATE articleRevision SET revisionNumber = :revisionNumber " +
            "WHERE revisionId = :revisionId");
        updateRevision.setParameter("revisionNumber", revisionNumber);
        updateRevision.setParameter("revisionId", existingRevisionId);
        return updateRevision.executeUpdate();
      } else {
        SQLQuery insertRevision = session.createSQLQuery("" +
            "INSERT INTO articleRevision (ingestionId, revisionNumber) " +
            "VALUES (:ingestionId, :revisionNumber)");
        insertRevision.setParameter("ingestionId", ingestionId);
        insertRevision.setParameter("revisionNumber", revisionNumber);
        insertRevision.executeUpdate();
        return getLastInsertId(session);
      }
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

  private void persistItem(ArticlePackage articlePackage, long ingestionId) {
    for (ArticleItemInput work : articlePackage.getAllWorks()) {
      persistItem(work, ingestionId);
    }
    persistArchivalFiles(articlePackage, ingestionId);
  }

  private long persistItem(ArticleItemInput work, long ingestionId) {
    Map<String, RepoVersion> crepoResults = new LinkedHashMap<>();
    for (Map.Entry<String, RepoObject> entry : work.getObjects().entrySet()) {
      RepoVersion result = parentService.contentRepoService.autoCreateRepoObject(entry.getValue()).getVersion();
      crepoResults.put(entry.getKey(), result);
    }

    return parentService.hibernateTemplate.execute(session -> {
      SQLQuery insertWork = session.createSQLQuery("" +
          "INSERT INTO articleItem (ingestionId, doi, articleItemType) " +
          "  VALUES (:ingestionId, :doi, :articleItemType)");
      insertWork.setParameter("ingestionId", ingestionId);
      insertWork.setParameter("doi", work.getDoi().getIdentifier());
      insertWork.setParameter("articleItemType", work.getType());
      insertWork.executeUpdate();
      long itemId = getLastInsertId(session);

      for (Map.Entry<String, RepoVersion> entry : crepoResults.entrySet()) {
        SQLQuery insertFile = session.createSQLQuery("" +
            "INSERT INTO articleFile (ingestionId, itemId, fileType, crepoKey, crepoUuid) " +
            "  VALUES (:ingestionId, :itemId, :fileType, :crepoKey, :crepoUuid)");
        insertFile.setParameter("ingestionId", ingestionId);
        insertFile.setParameter("itemId", itemId);
        insertFile.setParameter("fileType", entry.getKey());

        RepoVersion repoVersion = entry.getValue();
        insertFile.setParameter("crepoKey", repoVersion.getKey());
        insertFile.setParameter("crepoUuid", repoVersion.getUuid().toString());

        insertFile.executeUpdate();
      }

      return itemId;
    });
  }

  private void persistArchivalFiles(ArticlePackage articlePackage, long ingestionId) {
    List<RepoVersion> archivalFiles = articlePackage.getArchivalFiles().stream()
        .map(archivalFile -> parentService.contentRepoService.autoCreateRepoObject(archivalFile).getVersion())
        .collect(Collectors.toList());
    parentService.hibernateTemplate.execute(session -> {
      for (RepoVersion archivalFile : archivalFiles) {
        SQLQuery insertFile = session.createSQLQuery("" +
            "INSERT INTO articleFile (ingestionId, crepoKey, crepoUuid) " +
            "  VALUES (:ingestionId, :crepoKey, :crepoUuid)");
        insertFile.setParameter("ingestionId", ingestionId);
        insertFile.setParameter("crepoKey", archivalFile.getKey());
        insertFile.setParameter("crepoUuid", archivalFile.getUuid());
        insertFile.executeUpdate();
      }
      return null;
    });
  }

  private long persistJournal(Article article, long ingestionId) {
    Journal publicationJournal = parentService.getPublicationJournal(article);
    return parentService.hibernateTemplate.execute(session -> {
      SQLQuery query = session.createSQLQuery("" +
          "INSERT INTO articleJournalJoinTable (ingestionId, journalId) " +
          "VALUES (:ingestionId, :journalId)");
      query.setParameter("ingestionId", ingestionId);
      query.setParameter("journalId", publicationJournal.getJournalID());
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
  Article getArticleMetadata(ArticleRevisionIdentifier revisionId, ArticleMetadataSource source) {
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

    ArticleItem work = parentService.getArticleItem(revisionId.getItemFor());

    ArticleFile manuscriptVersion = work.getFile("manuscript").orElseThrow(() -> {
      String message = String.format("Work exists but does not have a manuscript. DOI: %s. Revision: %s",
          work.getDoi(), work.getIngestion().getIngestionNumber());
      return new RestClientException(message, HttpStatus.BAD_REQUEST);
    });

    Document document;
    try (InputStream manuscriptStream = parentService.contentRepoService.getRepoObject(manuscriptVersion.getCrepoVersion())) {
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
