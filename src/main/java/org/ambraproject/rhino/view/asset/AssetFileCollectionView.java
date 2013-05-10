package org.ambraproject.rhino.view.asset;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.view.JsonOutputView;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.List;

/**
 * A serializable view of a group of assets that all have the same DOI, possibly representing different files.
 */
public class AssetFileCollectionView implements JsonOutputView {

  private final ImmutableList<ArticleAsset> assets;

  public AssetFileCollectionView(Collection<? extends ArticleAsset> assets) {
    this.assets = ImmutableList.copyOf(assets);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    return serializeAssetFiles(assets, context);
  }

  /*
   * Simple serialization. This method is a hook in case something more complex is needed.
   */
  private static JsonObject serializeAsset(ArticleAsset asset, JsonSerializationContext context) {
    return context.serialize(asset).getAsJsonObject();
  }

  /* package-private */
  static JsonElement serializeAssetFiles(List<ArticleAsset> assets, JsonSerializationContext context) {
    if (assets.isEmpty()) {
      return new JsonArray();
    }
    if (assets.size() == 1) {
      ArticleAsset only = assets.get(0);
      if (only.getExtension().isEmpty()) {
        // Just one uninitialized asset.
        // It doesn't have a file identity, so return it in a plain array instead of keyed by file ID.
        JsonArray uninitialized = new JsonArray();
        uninitialized.add(serializeAsset(only, context));
        return uninitialized;
      }
    }
    return serializeInitializedAssetFiles(assets, context);
  }

  /*
   * A set of assets with associated files. Key them by file ID. Expect none to be uninitialized.
   */
  private static JsonObject serializeInitializedAssetFiles(Collection<ArticleAsset> assets, JsonSerializationContext context) {
    JsonObject byAssetFileId = new JsonObject();
    String commonAssetId = null;
    for (ArticleAsset asset : assets) {
      String assetId = DoiBasedIdentity.removeScheme(asset.getDoi());
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

      byAssetFileId.add(assetFileId, serializeAsset(asset, context));
    }
    return byAssetFileId;
  }

}
