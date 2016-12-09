package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.CustomMetadataExtractor;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.article.ArticleCustomMetadata;
import org.ambraproject.rhino.model.article.ArticleMetadata;
import org.ambraproject.rhino.model.ingest.ArticlePackage;
import org.ambraproject.rhino.model.ingest.ArticlePackageBuilder;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.util.Archive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class IngestionService extends AmbraService {

  @Autowired
  private CustomMetadataExtractor.Factory customMetadataExtractorFactory;
  @Autowired
  private ArticleValidationService articleValidationService;
  @Autowired
  private HibernatePersistenceService hibernatePersistenceService;

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

    articleValidationService.validateManifestCompleteness(manifestXml, archive);

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
    CustomMetadataExtractor customMetadataExtractor;
    try (InputStream manuscriptStream = new BufferedInputStream(archive.openFile(manuscriptEntry))) {
      Document document = AmbraService.parseXml(manuscriptStream);
      parsedArticle = new ArticleXml(document);
      customMetadataExtractor = customMetadataExtractorFactory.parse(document);
    }
    final ArticleMetadata articleMetadata = parsedArticle.build();
    ArticleCustomMetadata customMetadata = customMetadataExtractor.build();
    ArticleIdentifier articleIdentifier = ArticleIdentifier.create(articleMetadata.getDoi());
    Doi doi = articleIdentifier.getDoi();

    for (ManifestXml.Asset asset : assets) {
      articleValidationService.validateAssetUniqueness(asset, doi);
    }

    if (!doi.equals(Doi.create(manuscriptAsset.getUri()))) {
      String message = String.format("Article DOI is inconsistent. From manifest: \"%s\" From manuscript: \"%s\"",
          manuscriptAsset.getUri(), doi.getName());
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }

    Article article = hibernatePersistenceService.persistArticle(articleIdentifier);
    ArticleIngestion ingestion = hibernatePersistenceService.persistIngestion(article,
        articleMetadata, customMetadata);

    // TODO: Allow bucket name to be specified as an ingestion parameter
    String destinationBucketName = runtimeConfiguration.getCorpusStorage().getDefaultBucket();

    ArticlePackage articlePackage = new ArticlePackageBuilder(destinationBucketName, archive, parsedArticle, manifestXml,
        manuscriptAsset, manuscriptRepr, printableRepr).build();
    articleValidationService.validateAssetCompleteness(parsedArticle, articlePackage);
    hibernatePersistenceService.persistAssets(articlePackage, ingestion, manifestXml);

    hibernateTemplate.flush();
    hibernateTemplate.refresh(ingestion); // Pick up auto-persisted timestamp
    return ingestion;
  }
}
