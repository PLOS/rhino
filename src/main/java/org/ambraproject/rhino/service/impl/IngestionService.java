package org.ambraproject.rhino.service.impl;

import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.CustomMetadataExtractor;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.article.ArticleCustomMetadata;
import org.ambraproject.rhino.model.article.ArticleMetadata;
import org.ambraproject.rhino.model.ingest.ArticlePackage;
import org.ambraproject.rhino.model.ingest.ArticlePackageBuilder;
import org.ambraproject.rhino.model.ingest.IngestPackage;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.util.Archive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class IngestionService extends AmbraService {

  @Autowired
  private CustomMetadataExtractor.Factory customMetadataExtractorFactory;
  @Autowired
  private HibernatePersistenceService hibernatePersistenceService;
  @Autowired
  private ArticleCrudService articleCrudService;

  public ArticleIngestion ingest(Archive archive) throws IOException, XmlContentException {
    IngestPackage ingestPackage = createIngestPackage(archive);
    return processIngestPackage(ingestPackage);
  }

  private IngestPackage createIngestPackage(Archive archive) throws IOException {
    ManifestXml manifestXml = getManifestXml(archive);

    validateManifest(archive, manifestXml);

    String manuscriptEntry = getManuscriptEntry(archive, manifestXml);

    Document document = getDocument(archive, manuscriptEntry);

    ArticleXml parsedArticle = new ArticleXml(document);
    ArticleCustomMetadata customMetadata = customMetadataExtractorFactory.parse(document).build();

    // TODO: Allow bucket name to be specified as an ingestion parameter
    String destinationBucketName = runtimeConfiguration.getCorpusStorage().getDefaultBucket();

    ArticlePackage articlePackage = new ArticlePackageBuilder(destinationBucketName, archive,
        parsedArticle, manifestXml).build();

    articlePackage.validateAssetCompleteness(parsedArticle);

    ArticleMetadata articleMetadata = parsedArticle.build();
    return new IngestPackage(articlePackage, articleMetadata, customMetadata);
  }

  private ArticleIngestion processIngestPackage(IngestPackage ingestPackage) {
    Doi doi = ArticleIdentifier.create(ingestPackage.getArticleMetadata().getDoi()).getDoi();

    ArticlePackage articlePackage = ingestPackage.getArticlePackage();

    validateAssets(articlePackage, doi);
    validateManuscript(doi, articlePackage);

    return persistArticle(ingestPackage, doi, articlePackage);
  }

  private Document getDocument(Archive archive, String manuscriptEntry) throws IOException {
    Document document;
    try (InputStream manuscriptStream = new BufferedInputStream(archive.openFile(manuscriptEntry))) {
      document = AmbraService.parseXml(manuscriptStream);
    }
    return document;
  }

  private String getManuscriptEntry(Archive archive, ManifestXml manifestXml) {
    ManifestXml.Representation manuscriptRepr = manifestXml.getArticleAsset()
        .getRepresentation("manuscript")
        .orElseThrow(() -> new RestClientException("Manuscript entry not found in manifest",
            HttpStatus.BAD_REQUEST));

    String manuscriptEntry = manuscriptRepr.getFile().getEntry();
    if (!archive.getEntryNames().contains(manuscriptEntry)) {
      throw new RestClientException("Manuscript file not found in archive: " + manuscriptEntry,
          HttpStatus.BAD_REQUEST);
    }
    return manuscriptEntry;
  }

  private void validateManifest(Archive archive, ManifestXml manifestXml) {
    manifestXml.validateManifestCompleteness(archive);
  }

  private ManifestXml getManifestXml(Archive archive) throws IOException {
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
    return manifestXml;
  }

  private void validateManuscript(Doi doi, ArticlePackage articlePackage) {
    String manuscriptAssetUri = articlePackage.getManifest().getArticleAsset().getUri();
    if (!doi.equals(Doi.create(manuscriptAssetUri))) {
      String message = String.format("Article DOI is inconsistent. From manifest: \"%s\" From manuscript: \"%s\"",
          manuscriptAssetUri, doi.getName());
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }
  }

  private ArticleIngestion persistArticle(IngestPackage ingestPackage, Doi doi,
                                          ArticlePackage articlePackage) {
    Article article = hibernatePersistenceService.persistArticle(doi);
    ArticleIngestion ingestion = hibernatePersistenceService.persistIngestion(article,
        ingestPackage.getArticleMetadata(), ingestPackage.getArticleCustomMetadata());

    hibernatePersistenceService.persistAssets(articlePackage, ingestion);

    hibernateTemplate.flush();
    hibernateTemplate.refresh(ingestion); // Pick up auto-persisted timestamp
    return ingestion;
  }

  private void validateAssets(ArticlePackage articlePackage, Doi articleDoi) {
    for (ManifestXml.Asset asset : articlePackage.getManifest().getAssets()) {
      validateAssetUniqueness(articleDoi, asset);
    }
  }

  private void validateAssetUniqueness(Doi articleDoi, ManifestXml.Asset asset) {
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
}
