package org.ambraproject.rhino.identity;

import org.ambraproject.models.ArticleAsset;

/**
 * An identity for an asset, independent of its associated files.
 */
public class AssetIdentity extends DoiBasedIdentity {

  protected AssetIdentity(String identifier) {
    super(identifier);
  }

  public static AssetIdentity create(String identifier) {
    return new AssetIdentity(identifier);
  }

  /**
   * Create an identifier that refers to an asset. The returned object identifies the group of asset files belonging to
   * the {@link ArticleAsset} entity's DOI, independent of which individual file is represented by that entity.
   *
   * @param asset an Ambra asset entity
   * @return an identifier for the asset
   */
  public static AssetIdentity from(ArticleAsset asset) {
    return AssetIdentity.create(asIdentifier(asset.getDoi()));
  }

}
