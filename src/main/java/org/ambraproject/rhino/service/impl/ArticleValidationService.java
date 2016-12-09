package org.ambraproject.rhino.service.impl;

import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.ingest.ArticlePackage;
import org.ambraproject.rhino.util.Archive;

public interface ArticleValidationService {

  /**
   * Validate that all assets mentioned in the manuscript were included in the package.
   *
   * @param manuscript
   * @param articlePackage
   */
  public void validateAssetCompleteness(ArticleXml manuscript, ArticlePackage articlePackage);

  /**
   * Validate that all assets have the correct DOI for the article they are supposed to belong to.
   *
   * @param asset
   * @param articleDoi
   */
  public void validateAssetUniqueness(ManifestXml.Asset asset, Doi articleDoi);

  /**
   * Validate that all assets mentioned in the manifest are present in the archive.
   *
   * @param manifest
   * @param archive
   */
  public void validateManifestCompleteness(ManifestXml manifest, Archive archive);

}
