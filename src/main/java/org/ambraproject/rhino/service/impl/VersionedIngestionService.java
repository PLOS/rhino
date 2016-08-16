package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.article.ArticleMetadata;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.util.Archive;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.plos.crepo.model.identity.RepoId;
import org.plos.crepo.model.identity.RepoVersion;
import org.plos.crepo.model.input.RepoObjectInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VersionedIngestionService extends AmbraService {

  private static final Logger log = LoggerFactory.getLogger(VersionedIngestionService.class);

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private JournalCrudService journalCrudService;


  private static long getLastInsertId(Session session) {
    return ((Number) session.createSQLQuery("SELECT LAST_INSERT_ID()").uniqueResult()).longValue();
  }

  public ArticleIngestionIdentifier ingest(Archive archive) throws IOException, XmlContentException {
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

    // TODO: reinstate the validation check when AccMan friendly zip files become more readily available
    // validateManifestCompleteness(manifestXml, archive);
    log.warn("*** Skipping validation of manifest to accommodate ingestion of legacy zip files ***");

    ImmutableList<ManifestXml.Asset> assets = manifestXml.getAssets();
    ManifestXml.Asset manuscriptAsset = findManuscriptAsset(assets);
    ManifestXml.Representation manuscriptRepr = findManuscriptRepr(manuscriptAsset);
    Optional<ManifestXml.Representation> printableRepr = findPrintableRepr(manuscriptAsset);

    String manuscriptEntry = manuscriptRepr.getFile().getEntry();
    if (!archive.getEntryNames().contains(manifestEntry)) {
      throw new RestClientException("Manifest refers to missing file as main-entry: " + manuscriptEntry, HttpStatus.BAD_REQUEST);
    }

    ArticleXml parsedArticle;
    try (InputStream manuscriptStream = new BufferedInputStream(archive.openFile(manuscriptEntry))) {
      parsedArticle = new ArticleXml(AmbraService.parseXml(manuscriptStream));
    }
    final ArticleMetadata articleMetadata = parsedArticle.build();
    ArticleIdentifier articleIdentifier = ArticleIdentifier.create(articleMetadata.getDoi());
    Doi doi = articleIdentifier.getDoi();

    for (ManifestXml.Asset asset : assets) {
      validateAssetUniqueness(asset, doi);
    }

    if (!doi.equals(Doi.create(manuscriptAsset.getUri()))) {
      String message = String.format("Article DOI is inconsistent. From manifest: \"%s\" From manuscript: \"%s\"",
          manuscriptAsset.getUri(), doi.getName());
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }

    long articlePk = persistArticlePk(articleIdentifier);
    IngestionPersistenceResult ingestionResult = persistIngestion(articlePk, articleMetadata);
    long ingestionId = ingestionResult.pk;

    // TODO: Allow bucket name to be specified as an ingestion parameter
    String destinationBucketName = runtimeConfiguration.getCorpusStorage().getDefaultBucket();

    ArticlePackage articlePackage = new ArticlePackageBuilder(destinationBucketName, archive, parsedArticle, manifestXml,
        manuscriptAsset, manuscriptRepr, printableRepr).build();
    persistItem(articlePackage, ingestionId);
    persistJournal(articleMetadata, ingestionId);

    return ArticleIngestionIdentifier.create(doi, ingestionResult.ingestionNumber);
  }

  /**
   * Get the PK of the "article" row for a DOI if it exists, and insert it if it doesn't.
   *
   * @param articleIdentifier
   */
  private long persistArticlePk(ArticleIdentifier articleIdentifier) {
    String articleDoi = articleIdentifier.getDoiName();
    return hibernateTemplate.execute(session -> {
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

  private static class IngestionPersistenceResult {
    private final long pk;
    private final int ingestionNumber;

    private IngestionPersistenceResult(long pk, int ingestionNumber) {
      this.pk = pk;
      this.ingestionNumber = ingestionNumber;
    }
  }

  private IngestionPersistenceResult persistIngestion(long articlePk, ArticleMetadata articleMetadata) {
    return hibernateTemplate.execute(session -> {
      SQLQuery findNextIngestionNumber = session.createSQLQuery(
          "SELECT MAX(ingestionNumber) FROM articleIngestion WHERE articleId = :articleId");
      findNextIngestionNumber.setParameter("articleId", articlePk);
      Number maxIngestionNumber = (Number) findNextIngestionNumber.uniqueResult();
      int nextIngestionNumber = (maxIngestionNumber == null) ? FIRST_INGESTION_NUMBER
          : maxIngestionNumber.intValue() + 1;

      SQLQuery insertEvent = session.createSQLQuery("" +
          "INSERT INTO articleIngestion (articleId, ingestionNumber, title, publicationDate) " +
      "VALUES (:articleId, :ingestionNumber, :title, :publicationDate)");
      insertEvent.setParameter("articleId", articlePk);
      insertEvent.setParameter("ingestionNumber", nextIngestionNumber);
      insertEvent.setParameter("title", articleMetadata.getTitle());
      insertEvent.setParameter("publicationDate", java.sql.Date.valueOf(articleMetadata.getPublicationDate()));
      insertEvent.executeUpdate();
      long pk = getLastInsertId(session);

      return new IngestionPersistenceResult(pk, nextIngestionNumber);
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

  private void validateAssetUniqueness(ManifestXml.Asset asset, Doi articleDoi) {
    Doi assetDoi = Doi.create(asset.getUri());
    for (ArticleItem existingItem : articleCrudService.getAllArticleItems(assetDoi)) {
      ArticleTable existingParentArticle = existingItem.getIngestion().getArticle();
      if (!Doi.create(existingParentArticle.getDoi()).equals(articleDoi)) {
        String errorMessage = String.format("Incoming article ingestion (doi:%s) has a duplicate " +
            "article asset (doi:%s). Duplicate asset belongs to article doi: %s.",
            articleDoi.getName(), assetDoi, existingParentArticle.getDoi());
        throw new RestClientException(errorMessage, HttpStatus.BAD_REQUEST);
      }
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
    for (Map.Entry<String, RepoObjectInput> entry : work.getObjects().entrySet()) {
      RepoVersion result = contentRepoService.autoCreateRepoObject(entry.getValue()).getVersion();
      crepoResults.put(entry.getKey(), result);
    }

    return hibernateTemplate.execute(session -> {
      SQLQuery insertWork = session.createSQLQuery("" +
          "INSERT INTO articleItem (ingestionId, doi, articleItemType) " +
          "  VALUES (:ingestionId, :doi, :articleItemType)");
      insertWork.setParameter("ingestionId", ingestionId);
      insertWork.setParameter("doi", work.getDoi().getName());
      insertWork.setParameter("articleItemType", work.getType());
      insertWork.executeUpdate();
      long itemId = getLastInsertId(session);

      for (Map.Entry<String, RepoVersion> entry : crepoResults.entrySet()) {
        SQLQuery insertFile = session.createSQLQuery("" +
            "INSERT INTO articleFile (ingestionId, itemId, fileType, bucketName, crepoKey, crepoUuid) " +
            "  VALUES (:ingestionId, :itemId, :fileType, :bucketName, :crepoKey, :crepoUuid)");
        insertFile.setParameter("ingestionId", ingestionId);
        insertFile.setParameter("itemId", itemId);
        insertFile.setParameter("fileType", entry.getKey());

        RepoVersion repoVersion = entry.getValue();
        RepoId repoId = repoVersion.getId();
        insertFile.setParameter("bucketName", repoId.getBucketName());
        insertFile.setParameter("crepoKey", repoId.getKey());
        insertFile.setParameter("crepoUuid", repoVersion.getUuid().toString());

        insertFile.executeUpdate();
      }

      return itemId;
    });
  }

  private void persistArchivalFiles(ArticlePackage articlePackage, long ingestionId) {
    List<RepoVersion> archivalFiles = articlePackage.getArchivalFiles().stream()
        .map(archivalFile -> contentRepoService.autoCreateRepoObject(archivalFile).getVersion())
        .collect(Collectors.toList());
    hibernateTemplate.execute(session -> {
      for (RepoVersion archivalFile : archivalFiles) {
        SQLQuery insertFile = session.createSQLQuery("" +
            "INSERT INTO articleFile (ingestionId, bucketName, crepoKey, crepoUuid) " +
            "  VALUES (:ingestionId, :bucketName, :crepoKey, :crepoUuid)");
        insertFile.setParameter("ingestionId", ingestionId);
        RepoId repoId = archivalFile.getId();
        insertFile.setParameter("bucketName", repoId.getBucketName());
        insertFile.setParameter("crepoKey", repoId.getKey());
        insertFile.setParameter("crepoUuid", archivalFile.getUuid().toString());
        insertFile.executeUpdate();
      }
      return null;
    });
  }

  private long persistJournal(ArticleMetadata article, long ingestionId) {
    String eissn = article.geteIssn();
    if (eissn == null) {
      String msg = "eIssn not set for article: " + article.getDoi();
      throw new RestClientException(msg, HttpStatus.BAD_REQUEST);
    }
    Journal journal = journalCrudService.getJournalByEissn(eissn).orElseThrow(() -> {
      String msg = "XML contained eIssn that was not matched to a journal: " + eissn;
      return new RestClientException(msg, HttpStatus.BAD_REQUEST);
    });

    return hibernateTemplate.execute(session -> {
      SQLQuery query = session.createSQLQuery("" +
          "INSERT INTO articleJournalJoinTable (ingestionId, journalId) " +
          "VALUES (:ingestionId, :journalId)");
      query.setParameter("ingestionId", ingestionId);
      query.setParameter("journalId", journal.getJournalId());
      query.executeUpdate();
      return getLastInsertId(session);
    });
  }


  private ManifestXml.Asset findManuscriptAsset(List<ManifestXml.Asset> assets) {
    for (ManifestXml.Asset asset : assets) {
      if (asset.getMainEntry().isPresent()) {
        return asset;
      }
    }
    throw new RestClientException("main-entry not found", HttpStatus.BAD_REQUEST);
  }

  private Optional<ManifestXml.Representation> findPrintableRepr(ManifestXml.Asset manuscriptAsset) {
    Preconditions.checkArgument(manuscriptAsset.getMainEntry().isPresent(), "manuscriptAsset must have main-entry");
    return manuscriptAsset.getRepresentations().stream()
        .filter(representation -> representation.getName().equals("PDF"))
        .findAny();
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
   * @param ingestionId the ID of the article to serve
   * @return an object containing metadata that could be extracted from the manuscript, with other fields unfilled
   */
  public ArticleMetadata getArticleMetadata(ArticleIngestionIdentifier ingestionId) {
    ArticleIngestion ingestion = articleCrudService.readIngestion(ingestionId);

    Document document = articleCrudService.getManuscriptXml(ingestion);

    try {
      return new ArticleXml(document).build();
    } catch (XmlContentException e) {
      throw new RuntimeException(e);
    }
  }

  public Archive repack(ArticleIdentity articleIdentity) {
    throw new UnsupportedOperationException(); // TODO
  }
}
