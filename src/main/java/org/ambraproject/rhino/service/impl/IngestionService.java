/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.service.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import org.ambraproject.rhino.config.RuntimeConfiguration;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public class IngestionService extends AmbraService {

  public static final String MANIFEST_XML = "manifest.xml";

  @Autowired
  private CustomMetadataExtractor.Factory customMetadataExtractorFactory;
  @Autowired
  private HibernatePersistenceService hibernatePersistenceService;
  @Autowired
  private ArticleCrudService articleCrudService;

  public ArticleIngestion ingest(Archive archive, Optional<String> bucketName)
      throws IOException, XmlContentException {
    IngestPackage ingestPackage = createIngestPackage(archive, bucketName);
    return processIngestPackage(ingestPackage);
  }

  private IngestPackage createIngestPackage(Archive archive, Optional<String> bucketName)
      throws IOException {
    ManifestXml manifestXml = getManifestXml(archive);

    ImmutableSet<String> entryNames = archive.getEntryNames();
    manifestXml.validateManifestCompleteness(entryNames);

    String manuscriptEntry = getManuscriptEntry(entryNames, manifestXml);

    Document document = getDocument(archive, manuscriptEntry);

    ArticleXml parsedArticle = new ArticleXml(document);
    ArticleCustomMetadata customMetadata = customMetadataExtractorFactory.parse(document).build();

    ArticlePackage articlePackage = new ArticlePackageBuilder(resolveBucketName(bucketName),
        archive, parsedArticle, manifestXml).build();

    articlePackage.validateAssetCompleteness(parsedArticle.findAllAssetNodes().getDois());

    ArticleMetadata articleMetadata = parsedArticle.build();
    return new IngestPackage(articlePackage, articleMetadata, customMetadata);
  }

  /**
   * Validate the bucket name against the set of allowed buckets and supply the default if needed.
   *
   * @param bucketName the bucket name specified as the destination for this ingestion, or empty if the client did not
   *                   specify a bucket name
   * @return the specified bucket name, or the default if the client did not specify a bucket name
   * @throws RestClientException if the clietn specified a disallowed (or nonexistent) bucket name
   */
  private String resolveBucketName(Optional<String> bucketName) {
    RuntimeConfiguration.MultiBucketContentRepoEndpoint corpusStorage = runtimeConfiguration.getCorpusStorage();
    if (!bucketName.isPresent()) {
      return corpusStorage.getDefaultBucket();
    }

    String configuredName = bucketName.get();
    Set<String> allowedBuckets = corpusStorage.getAllBuckets();
    if (!allowedBuckets.contains(configuredName)) {
      String message = String.format("" +
              "Invalid bucket name: %s. Allowed values are: %s. " +
              "(Allowed values are specified by rhino.yaml.)",
          configuredName, allowedBuckets);
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }
    return configuredName;
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

  /**
   * Loads the manuscript referenced in the archive.
   *
   * @param archive The archive
   * @param manuscriptEntry The manuscript entry
   *
   * @return The manuscript
   *
   * @throws IOException if unable to load manuscript
   */
  @VisibleForTesting
  Document getDocument(Archive archive, String manuscriptEntry) throws IOException {
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
    final ImmutableSet<String> entryNames = archive.getEntryNames();
    if (!entryNames.contains(MANIFEST_XML)) {
      throw new RestClientException("Archive has no manifest file", HttpStatus.BAD_REQUEST);
    }

    ManifestXml manifestXml;
    try (InputStream manifestStream =
        new BufferedInputStream(archive.openFile(MANIFEST_XML))) {
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
    ArticleIngestion ingestion = hibernatePersistenceService.persistIngestion(article, ingestPackage);

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
