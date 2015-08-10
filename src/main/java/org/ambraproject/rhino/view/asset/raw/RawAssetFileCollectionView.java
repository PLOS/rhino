package org.ambraproject.rhino.view.asset.raw;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.article.ArticleVisibility;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * A serializable view of a group of assets that all have the same DOI, possibly representing different files.
 */
public class RawAssetFileCollectionView implements JsonOutputView {

  private final ImmutableList<ArticleAsset> assets;
  private final ArticleVisibility parentArticle;

  public RawAssetFileCollectionView(Collection<? extends ArticleAsset> assets, ArticleVisibility parentArticle) {
    this.assets = ImmutableList.copyOf(assets);
    this.parentArticle = Preconditions.checkNotNull(parentArticle);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    return serializeAssetFiles(assets, parentArticle, context);
  }

  /* package-private */
  static JsonElement serializeAssetFiles(List<ArticleAsset> assets,
                                         ArticleVisibility parentArticle,
                                         JsonSerializationContext context) {
    if (assets.isEmpty()) {
      return new JsonArray();
    }
    if (assets.size() == 1) {
      ArticleAsset only = assets.get(0);
      if (only.getExtension().isEmpty()) {
        // Just one uninitialized asset.
        // It doesn't have a file identity, so return it in a plain array instead of keyed by file ID.
        JsonArray uninitialized = new JsonArray();
        JsonElement serializedAsset = context.serialize(new RawAssetFileView(only, parentArticle));
        uninitialized.add(serializedAsset);
        return uninitialized;
      }
    }
    return serializeInitializedAssetFiles(assets, parentArticle, context);
  }

  /*
   * A set of assets with associated files. Key them by file ID. Expect none to be uninitialized.
   */
  private static JsonObject serializeInitializedAssetFiles(Collection<ArticleAsset> assets,
                                                           ArticleVisibility parentArticle,
                                                           JsonSerializationContext context) {
    JsonObject byAssetFileId = new JsonObject();
    String commonAssetId = null;
    for (ArticleAsset asset : assets) {
      String assetId = DoiBasedIdentity.asIdentifier(asset.getDoi());
      if (commonAssetId == null) {
        commonAssetId = assetId;
      } else if (!commonAssetId.equals(assetId)) {
        String message = String.format("Mismatched asset identities: \"%s\", \"%s\"", commonAssetId, assetId);
        throw new IllegalArgumentException(message);
      }

      String extension = asset.getExtension();
      if (StringUtils.isBlank(extension)) {
        throw new IllegalArgumentException("Expected only initialized files");
      }
      String assetFileId = assetId + '.' + extension;
      if (byAssetFileId.has(assetFileId)) {
        // Data integrity rules should make this impossible.
        throw new RuntimeException("Asset file ID collision: " + assetFileId);
      }

      JsonElement serializedAsset = context.serialize(new RawAssetFileView(asset, parentArticle));
      byAssetFileId.add(assetFileId, serializedAsset);
    }
    return byAssetFileId;
  }

}
