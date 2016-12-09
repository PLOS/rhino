package org.ambraproject.rhino.service.impl;

import com.google.common.collect.Sets;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ingest.ArticleItemInput;
import org.ambraproject.rhino.model.ingest.ArticlePackage;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.util.Archive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArticleValidationServiceImpl implements ArticleValidationService {

  @Autowired
  private ArticleCrudService articleCrudService;

  @Override
  public void validateAssetCompleteness(ArticleXml manuscript, ArticlePackage articlePackage) {
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

  @Override
  public void validateAssetUniqueness(ManifestXml.Asset asset, Doi articleDoi) {
    Doi assetDoi = Doi.create(asset.getUri());
    for (ArticleItem existingItem : articleCrudService.getAllArticleItems(assetDoi)) {
      Article existingParentArticle = existingItem.getIngestion().getArticle();
      if (!Doi.create(existingParentArticle.getDoi()).equals(articleDoi)) {
        String errorMessage = String.format("Incoming article ingestion (doi:%s) has a duplicate " +
                "article asset (doi:%s). Duplicate asset belongs to article doi: %s.",
            articleDoi.getName(), assetDoi, existingParentArticle.getDoi());
        throw new RestClientException(errorMessage, HttpStatus.BAD_REQUEST);
      }
    }  }

  @Override
  public void validateManifestCompleteness(ManifestXml manifest, Archive archive) {
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
}
