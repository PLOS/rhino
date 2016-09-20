package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.ArticleFile;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.article.ArticleMetadata;
import org.ambraproject.rhino.model.ingest.ArticleItemInput;
import org.ambraproject.rhino.model.ingest.ArticlePackage;
import org.ambraproject.rhino.model.ingest.ArticlePackageBuilder;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.util.Archive;
import org.hibernate.Query;
import org.plos.crepo.model.identity.RepoId;
import org.plos.crepo.model.identity.RepoVersion;
import org.plos.crepo.model.input.RepoObjectInput;
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

public class IngestionService extends AmbraService {

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private JournalCrudService journalCrudService;


  public ArticleIngestion ingest(Archive archive) throws IOException, XmlContentException {
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
    ManifestXml.Asset manuscriptAsset = manifestXml.getArticleAsset();
    ManifestXml.Representation manuscriptRepr = manuscriptAsset.getRepresentation("manuscript")
        .orElseThrow(() -> new RestClientException("Manuscript entry not found in manifest", HttpStatus.BAD_REQUEST));
    Optional<ManifestXml.Representation> printableRepr = manuscriptAsset.getRepresentation("printable");

    String manuscriptEntry = manuscriptRepr.getFile().getEntry();
    if (!archive.getEntryNames().contains(manifestEntry)) {
      throw new RestClientException("Manuscript file not found in archive: " + manuscriptEntry, HttpStatus.BAD_REQUEST);
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

    Article article = persistArticle(articleIdentifier);
    ArticleIngestion ingestion = persistIngestion(article, articleMetadata);

    // TODO: Allow bucket name to be specified as an ingestion parameter
    String destinationBucketName = runtimeConfiguration.getCorpusStorage().getDefaultBucket();

    ArticlePackage articlePackage = new ArticlePackageBuilder(destinationBucketName, archive, parsedArticle, manifestXml,
        manuscriptAsset, manuscriptRepr, printableRepr).build();
    validateAssetCompleteness(parsedArticle, articlePackage);
    persistAssets(articlePackage, ingestion, manifestXml);

    hibernateTemplate.flush();
    hibernateTemplate.refresh(ingestion); // Pick up auto-persisted timestamp
    return ingestion;
  }

  /**
   * Get the article object for a DOI if it exists, and save it if it doesn't.
   *
   * @param articleIdentifier
   */
  private Article persistArticle(ArticleIdentifier articleIdentifier) {
    String articleDoi = articleIdentifier.getDoiName();
    Article article = hibernateTemplate.execute(session -> {
      Query selectQuery = session.createQuery("FROM Article WHERE doi = :doi");
      selectQuery.setParameter("doi", articleDoi);
      return (Article) selectQuery.uniqueResult();
    });
    if (article == null) {
      article = new Article();
      article.setDoi(articleDoi);
      hibernateTemplate.save(article);
    }
    return article;
  }

  private static final int FIRST_INGESTION_NUMBER = 1;

  private ArticleIngestion persistIngestion(Article article, ArticleMetadata articleMetadata) {
    Journal journal = fetchJournal(articleMetadata);

    int nextIngestionNumber = hibernateTemplate.execute(session -> {
      Query findNextIngestionNumber = session.createQuery(
          "SELECT MAX(ingestionNumber) FROM ArticleIngestion WHERE article = :article");
      findNextIngestionNumber.setParameter("article", article);
      Number maxIngestionNumber = (Number) findNextIngestionNumber.uniqueResult();
      return (maxIngestionNumber == null) ? FIRST_INGESTION_NUMBER
          : maxIngestionNumber.intValue() + 1;
    });

    ArticleIngestion ingestion = new ArticleIngestion();
    ingestion.setArticle(article);
    ingestion.setIngestionNumber(nextIngestionNumber);
    ingestion.setTitle(articleMetadata.getTitle());
    ingestion.setPublicationDate(java.sql.Date.valueOf(articleMetadata.getPublicationDate()));
    ingestion.setRevisionDate((articleMetadata.getRevisionDate() == null ? null
        : java.sql.Date.valueOf(articleMetadata.getRevisionDate())));
    ingestion.setArticleType(articleMetadata.getArticleType());
    ingestion.setJournal(journal);

    hibernateTemplate.save(ingestion);
    return ingestion;
  }

  private void validateManifestCompleteness(ManifestXml manifest, Archive archive) {
    Set<String> archiveEntryNames = archive.getEntryNames();

    Stream<ManifestXml.ManifestFile> manifestFiles = Stream.concat(
        manifest.getAssets().stream()
            .flatMap(asset -> asset.getRepresentations().stream())
            .map(ManifestXml.Representation::getFile),
        manifest.getAncillaryFiles().stream());
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
      Article existingParentArticle = existingItem.getIngestion().getArticle();
      if (!Doi.create(existingParentArticle.getDoi()).equals(articleDoi)) {
        String errorMessage = String.format("Incoming article ingestion (doi:%s) has a duplicate " +
                "article asset (doi:%s). Duplicate asset belongs to article doi: %s.",
            articleDoi.getName(), assetDoi, existingParentArticle.getDoi());
        throw new RestClientException(errorMessage, HttpStatus.BAD_REQUEST);
      }
    }
  }

  /**
   * Validate that all assets mentioned in the manuscript were included in the package.
   */
  private void validateAssetCompleteness(ArticleXml manuscript, ArticlePackage articlePackage) {
    Set<Doi> manuscriptDois = manuscript.findAllAssetNodes().getDois();
    Set<Doi> packageDois = articlePackage.getAllItems().stream()
        .map(ArticleItemInput::getDoi)
        .collect(Collectors.toSet());
    Set<Doi> missingDois = Sets.difference(manuscriptDois, packageDois);
    if (!missingDois.isEmpty()) {
      String message = "Asset DOIs mentioned in manuscript are not included in package: "
          + missingDois.stream().map(Doi::getName).sorted().collect(Collectors.toList());
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Persist items, items' file representations, ancillary files, and the link to the striking image.
   */
  private void persistAssets(ArticlePackage articlePackage, ArticleIngestion ingestion, ManifestXml manifest) {
    List<ArticleItem> items = articlePackage.getAllItems().stream()
        .map((ArticleItemInput item) -> createItem(item, ingestion))
        .collect(Collectors.toList());
    for (ArticleItem item : items) {
      hibernateTemplate.save(item);
    }

    persistAncillaryFiles(articlePackage, ingestion);

    linkStrikingImage(ingestion, items, manifest);
  }

  private ArticleItem createItem(ArticleItemInput itemInput, ArticleIngestion ingestion) {
    Map<String, RepoVersion> crepoResults = new LinkedHashMap<>();
    for (Map.Entry<String, RepoObjectInput> entry : itemInput.getObjects().entrySet()) {
      RepoVersion result = contentRepoService.autoCreateRepoObject(entry.getValue()).getVersion();
      crepoResults.put(entry.getKey(), result);
    }

    ArticleItem item = new ArticleItem();
    item.setIngestion(ingestion);
    item.setDoi(itemInput.getDoi().getName());
    item.setItemType(itemInput.getType());

    item.setFiles(crepoResults.entrySet().stream().map((Map.Entry<String, RepoVersion> entry) -> {
      ArticleFile file = new ArticleFile();
      file.setIngestion(ingestion);
      file.setItem(item);
      file.setFileType(entry.getKey());

      RepoVersion repoVersion = entry.getValue();
      RepoId repoId = repoVersion.getId();
      file.setBucketName(repoId.getBucketName());
      file.setCrepoKey(repoId.getKey());
      file.setCrepoUuid(repoVersion.getUuid().toString());

      file.setFileSize(contentRepoService.getRepoObjectMetadata(repoVersion).getSize());

      return file;
    }).collect(Collectors.toList()));

    return item;
  }

  private List<ArticleFile> persistAncillaryFiles(ArticlePackage articlePackage, ArticleIngestion ingestion) {
    List<RepoVersion> ancillaryFiles = articlePackage.getAncillaryFiles().stream()
        .map(ancillaryFile -> contentRepoService.autoCreateRepoObject(ancillaryFile).getVersion())
        .collect(Collectors.toList());

    List<ArticleFile> fileObjects = ancillaryFiles.stream().map(repoVersion -> {
      ArticleFile file = new ArticleFile();
      file.setIngestion(ingestion);
      file.setFileSize(contentRepoService.getRepoObjectMetadata(repoVersion).getSize());

      RepoId repoId = repoVersion.getId();
      file.setBucketName(repoId.getBucketName());
      file.setCrepoKey(repoId.getKey());
      file.setCrepoUuid(repoVersion.getUuid().toString());
      return file;
    }).collect(Collectors.toList());

    for (ArticleFile fileObject : fileObjects) {
      hibernateTemplate.save(fileObject);
    }
    return fileObjects;
  }

  private Optional<ArticleItem> linkStrikingImage(ArticleIngestion ingestion, List<ArticleItem> items, ManifestXml manifest) {
    Optional<ManifestXml.Asset> strikingImageAsset = manifest.getAssets().stream()
        .filter(ManifestXml.Asset::isStrikingImage)
        .findAny();
    if (!strikingImageAsset.isPresent()) {
      return Optional.empty(); // No striking image declared, so go without setting one.
    }

    Doi strikingImageDoi = Doi.create(strikingImageAsset.get().getUri());
    Optional<ArticleItem> strikingImageItem = items.stream()
        .filter(item -> Doi.create(item.getDoi()).equals(strikingImageDoi))
        .findAny();
    if (!strikingImageItem.isPresent()) {
      throw new RuntimeException("Striking image from manifest not found (should have been created by now)");
    }

    ingestion.setStrikingImage(strikingImageItem.get());
    hibernateTemplate.update(ingestion);
    return strikingImageItem;
  }


  private Journal fetchJournal(ArticleMetadata article) {
    String eissn = article.geteIssn();
    if (eissn == null) {
      String msg = "eIssn not set for article: " + article.getDoi();
      throw new RestClientException(msg, HttpStatus.BAD_REQUEST);
    }
    return journalCrudService.getJournalByEissn(eissn).orElseThrow(() -> {
      String msg = "XML contained eIssn that was not matched to a journal: " + eissn;
      return new RestClientException(msg, HttpStatus.BAD_REQUEST);
    });
  }


  /**
   * Build a representation of an article's metadata from a persisted collection.
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

}