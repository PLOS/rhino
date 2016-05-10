package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.model.ScholarlyWork;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService.ArticleMetadataSource;
import org.ambraproject.rhino.util.Archive;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.plos.crepo.model.RepoObject;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.model.RepoVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

class VersionedIngestionService {

  private static final Logger log = LoggerFactory.getLogger(VersionedIngestionService.class);

  private final ArticleCrudServiceImpl parentService;

  VersionedIngestionService(ArticleCrudServiceImpl parentService) {
    this.parentService = Preconditions.checkNotNull(parentService);
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
    ImmutableList<ManifestXml.Asset> assets = manifestXml.getAssets();
    validateManifestCompleteness(assets, archive);

    ManifestXml.Asset manuscriptAsset = findManuscriptAsset(assets);
    ManifestXml.Representation manuscriptRepr = findManuscriptRepr(manuscriptAsset);
    ManifestXml.Representation printableRepr = findPrintableRepr(manuscriptAsset);

    String manuscriptEntry = manuscriptRepr.getEntry();
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
    final Article articleMetadata = parsedArticle.build(new Article());
    articleMetadata.setDoi(articleIdentity.getKey());

    ArticlePackage articlePackage = new ArticlePackageBuilder(archive, parsedArticle, manifestXml, manifestEntry, manuscriptRepr, printableRepr).build();
    Collection<Long> createdWorkPks = persist(articlePackage);

    persistRevision(articleIdentity, createdWorkPks, revision.orElseGet(parsedArticle::getRevisionNumber));

    stubAssociativeFields(articleMetadata);
    return articleMetadata;
  }

  private void validateManifestCompleteness(ImmutableList<ManifestXml.Asset> assets, Archive archive) {
    Set<String> archiveEntryNames = archive.getEntryNames();
    Set<String> manifestEntryNames = assets.stream()
        .flatMap(asset -> asset.getRepresentations().stream())
        .map(ManifestXml.Representation::getEntry)
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

  private Collection<Long> persist(ArticlePackage articlePackage) {
    Collection<Long> createdWorkPks = new ArrayList<>();

    ScholarlyWorkInput articleWork = articlePackage.getArticleWork();
    long articlePk = persistToSql(articleWork);
    createdWorkPks.add(articlePk);
    persistToCrepo(articleWork, articlePk);

    for (ScholarlyWorkInput assetWork : articlePackage.getAssetWorks()) {
      long assetPk = persistToSql(assetWork);
      persistToCrepo(assetWork, assetPk);
      createdWorkPks.add(assetPk);

      persistRelation(articlePk, assetPk);
    }

    return createdWorkPks;
  }

  /**
   * @param scholarlyWork the object to persist
   * @return the primary key of the inserted row
   */
  private long persistToSql(ScholarlyWorkInput scholarlyWork) {
    return parentService.hibernateTemplate.execute(session -> {
      SQLQuery query = session.createSQLQuery("" +
          "INSERT INTO scholarlyWork (doi, scholarlyWorkType)" +
          "  VALUES (:doi, :scholarlyWorkType)");
      query.setParameter("doi", scholarlyWork.getDoi().getIdentifier());
      query.setParameter("scholarlyWorkType", scholarlyWork.getType());
      query.executeUpdate();

      BigInteger scholarlyWorkId = (BigInteger) DataAccessUtils.requiredUniqueResult(session.createSQLQuery(
          "SELECT LAST_INSERT_ID()").list());
      return scholarlyWorkId.longValue();
    });
  }

  private void persistRelation(long articlePk, long assetPk) {
    parentService.hibernateTemplate.execute(session -> {
      SQLQuery query = session.createSQLQuery("" +
          "INSERT INTO scholarlyWorkRelation (originWorkId, targetWorkId, relationType) " +
          "VALUES (:originWorkId, :targetWorkId, :relationType)");
      query.setParameter("originWorkId", articlePk);
      query.setParameter("targetWorkId", assetPk);
      query.setParameter("relationType", "assetOf");

      return query.executeUpdate();
    });
  }

  private void persistToCrepo(ScholarlyWorkInput work, long workPk) {
    Map<String, RepoVersion> createdObjects = work.getObjects().entrySet()
        .parallelStream() // Parallelize writes to CRepo. Relies on side effects and must be thread-safe.
        .map((Map.Entry<String, RepoObject> entry) -> {
          RepoObjectMetadata createdObject = parentService.contentRepoService.autoCreateRepoObject(entry.getValue());
          return Maps.immutableEntry(entry.getKey(), createdObject.getVersion());
        })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    parentService.hibernateTemplate.execute(session -> {
      for (Map.Entry<String, RepoVersion> entry : createdObjects.entrySet()) {
        String fileType = entry.getKey();
        RepoVersion repoObjectPtr = entry.getValue();

        SQLQuery query = session.createSQLQuery("" +
            "INSERT INTO scholarlyWorkFile (scholarlyWorkId, fileType, crepoKey, crepoUuid) " +
            "  VALUES (:scholarlyWorkId, :fileType, :crepoKey, :crepoUuid)");
        query.setParameter("scholarlyWorkId", workPk);
        query.setParameter("fileType", fileType);
        query.setParameter("crepoKey", repoObjectPtr.getKey());
        query.setParameter("crepoUuid", repoObjectPtr.getUuid().toString());
        query.executeUpdate();
      }
      return null;
    });
  }

  private void persistRevision(ArticleIdentity articleDoi, Collection<Long> createdWorkPks, int revisionNumber) {
    // Delete assets
    parentService.hibernateTemplate.execute(session -> {
      Query query = session.createSQLQuery("" +
          "DELETE rev " +
          "FROM revision rev " +
          "INNER JOIN scholarlyWork asset ON rev.scholarlyWorkId = asset.scholarlyWorkId " +
          "INNER JOIN scholarlyWorkRelation rel ON asset.scholarlyWorkId = rel.targetWorkId " +
          "INNER JOIN scholarlyWork article ON article.scholarlyWorkId = rel.originWorkId " +
          "WHERE rev.revisionNumber = :revisionNumber AND article.doi = :doi");
      query.setParameter("revisionNumber", revisionNumber);
      query.setParameter("doi", articleDoi.getIdentifier());
      return query.executeUpdate();
    });

    // Delete root article
    parentService.hibernateTemplate.execute(session -> {
      Query query = session.createSQLQuery("" +
          "DELETE rev " +
          "FROM revision rev INNER JOIN scholarlyWork s " +
          "ON rev.scholarlyWorkId = s.scholarlyWorkId " +
          "WHERE rev.revisionNumber = :revisionNumber AND s.doi = :doi");
      query.setParameter("revisionNumber", revisionNumber);
      query.setParameter("doi", articleDoi.getIdentifier());
      return query.executeUpdate();
    });

    for (Long createdWorkPk : createdWorkPks) {
      parentService.hibernateTemplate.execute(session -> {
        Query query = session.createSQLQuery("" +
            "INSERT INTO revision (scholarlyWorkId, revisionNumber, publicationState) " +
            "  VALUES (:scholarlyWorkId, :revisionNumber, :publicationState)");
        query.setParameter("scholarlyWorkId", createdWorkPk);
        query.setParameter("revisionNumber", revisionNumber);
        query.setParameter("publicationState", 0);
        return query.executeUpdate();
      });
    }
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
      if (representation.getEntry().equals(mainEntry.get())) {
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
