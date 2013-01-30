package org.ambraproject.rhino.identity;

import org.ambraproject.models.ArticleAsset;

public class AssetIdentity extends DoiBasedIdentity {

  protected AssetIdentity(String identifier) {
    super(identifier);
  }

  public static AssetIdentity create(String identifier) {
    return new AssetIdentity(identifier);
  }

  /**
   * Create an identifier that refers to an asset. The argument is an Ambra entity that <em>may</em> be associated with
   * a particular file (i.e., it may have a non-empty extension), but the returned object will not specify a file
   * regardless.
   *
   * @param asset an Ambra asset entity
   * @return an identifier for the asset that ignored its associated file, if any
   */
  public static AssetIdentity from(ArticleAsset asset) {
    return AssetIdentity.create(removeScheme(asset.getDoi()));
  }

}
