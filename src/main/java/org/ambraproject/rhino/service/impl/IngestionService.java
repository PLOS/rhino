package org.ambraproject.rhino.service.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
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
import org.ambraproject.rhino.service.HibernatePersistenceService;
import org.ambraproject.rhino.util.Archive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

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

    ImmutableSet<String> entryNames = archive.getEntryNames();
    manifestXml.validateManifestCompleteness(entryNames);

    String manuscriptEntry = getManuscriptEntry(entryNames, manifestXml);

    Document document = getDocument(archive, manuscriptEntry);

    ArticleXml parsedArticle = new ArticleXml(document);
    ArticleCustomMetadata customMetadata = customMetadataExtractorFactory.parse(document).build();

    // TODO: Allow bucket name to be specified as an ingestion parameter
    String destinationBucketName = runtimeConfiguration.getCorpusStorage().getDefaultBucket();

    ArticlePackage articlePackage = new ArticlePackageBuilder(destinationBucketName, archive,
        parsedArticle, manifestXml).build();

    articlePackage.validateAssetCompleteness(parsedArticle.findAllAssetNodes().getDois());

    ArticleMetadata articleMetadata = parsedArticle.build();
    return new IngestPackage(articlePackage, articleMetadata, customMetadata);
  }

  private ArticleIngestion processIngestPackage(IngestPackage ingestPackage) {
    Doi doi = ArticleIdentifier.create(ingestPackage.getArticleMetadata().getDoi()).getDoi();

    ArticlePackage articlePackage = ingestPackage.getArticlePackage();

    for (ManifestXml.Asset asset : articlePackage.getManifest().getAssets()) {
      Doi assetDoi = Doi.create(asset.getUri());
      validateAssetUniqueness(doi, articleCrudService.getAllArticleItems(assetDoi));
    }
    validateManuscript(doi, articlePackage.getManifest().getArticleAsset().getUri());

    return persistArticle(ingestPackage, doi, articlePackage);
  }

  private Document getDocument(Archive archive, String manuscriptEntry) throws IOException {
    Document document;
    try (InputStream manuscriptStream = new BufferedInputStream(archive.openFile(manuscriptEntry))) {
      document = AmbraService.parseXml(manuscriptStream);
    }
    return document;
  }

  private String getManuscriptEntry(ImmutableSet<String> entryNames, ManifestXml manifestXml) {
    ManifestXml.Representation manuscriptRepr = manifestXml.getArticleAsset()
        .getRepresentation("manuscript")
        .orElseThrow(() -> new RestClientException("Manuscript entry not found in manifest",
            HttpStatus.BAD_REQUEST));

    String manuscriptEntry = manuscriptRepr.getFile().getEntry();
    if (!entryNames.contains(manuscriptEntry)) {
      throw new RestClientException("Manuscript file not found in archive: " + manuscriptEntry,
          HttpStatus.BAD_REQUEST);
    }
    return manuscriptEntry;
  }

  @VisibleForTesting
  ManifestXml getManifestXml(Archive archive) throws IOException {
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

  @VisibleForTesting
  void validateManuscript(Doi doi, String manuscriptAssetUri) {
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

  @VisibleForTesting
  void validateAssetUniqueness(Doi articleDoi, Collection<ArticleItem> articleItems) {
    for (ArticleItem existingItem : articleItems) {
      Article existingParentArticle = existingItem.getIngestion().getArticle();
      if (!Doi.create(existingParentArticle.getDoi()).equals(articleDoi)) {
        String errorMessage = String.format("Incoming article ingestion (doi:%s) has a duplicate " +
                "article asset. Duplicate asset belongs to article doi: %s.",
            articleDoi.getName(), existingParentArticle.getDoi());
        throw new RestClientException(errorMessage, HttpStatus.BAD_REQUEST);
      }
    }
  }
}
