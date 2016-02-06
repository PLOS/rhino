package org.ambraproject.rhino.identity;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.ambraproject.models.ArticleAsset;

/**
 * An identity for an asset, independent of its associated files (which may or may not be in the system).
 * <p/>
 * Rhino's model of assets, unlike previous versions of Ambra, permits an asset to exist in a state with no associated
 * files, either temporarily (such as a figure asset that has been found in article XML, but not had its image files
 * uploaded yet) or permanently (such as a future file-less asset such as an article correction). This class provides
 * static methods for setting and checking this state on {@link ArticleAsset} entities.
 */
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
    return AssetIdentity.create(asIdentifier(asset.getDoi()));
  }

}
