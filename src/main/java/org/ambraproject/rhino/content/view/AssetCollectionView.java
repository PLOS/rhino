package org.ambraproject.rhino.content.view;

import com.google.common.collect.ImmutableListMultimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

public class AssetCollectionView {

  private final ImmutableListMultimap<String, ArticleAsset> assets;

  public AssetCollectionView(Iterable<ArticleAsset> assets) {
    ImmutableListMultimap.Builder<String, ArticleAsset> buffer = ImmutableListMultimap.builder();
    for (ArticleAsset asset : assets) {
      buffer.put(asset.getDoi(), asset);
    }
    this.assets = buffer.build();
  }

  private static JsonObject serializeAsset(ArticleAsset asset, JsonSerializationContext context) {
    JsonObject jsonObject = context.serialize(asset).getAsJsonObject();

    String extension = asset.getExtension();
    if (StringUtils.isNotBlank(extension)) {
      String filename = ArticleIdentity.removeScheme(asset.getDoi()) + '.' + extension;
      jsonObject.addProperty("filename", filename);
    }

    return jsonObject;
  }

  public static final JsonSerializer<AssetCollectionView> SERIALIZER = new JsonSerializer<AssetCollectionView>() {
    @Override
    public JsonElement serialize(AssetCollectionView src, Type typeOfSrc, JsonSerializationContext context) {
      JsonObject byAssetId = new JsonObject();
      for (Map.Entry<String, Collection<ArticleAsset>> entry : src.assets.asMap().entrySet()) {
        String assetId = ArticleIdentity.removeScheme(entry.getKey());
        JsonObject byAssetFileId = new JsonObject();
        for (ArticleAsset asset : entry.getValue()) {
          String extension = asset.getExtension();
          String assetFileId = StringUtils.isBlank(extension) ? assetId : assetId + '.' + extension;
          if (byAssetFileId.has(assetFileId)) {
            // Data integrity rules should make this impossible.
            throw new RuntimeException("Asset file ID collision: " + assetFileId);
          }
          byAssetFileId.add(assetFileId, serializeAsset(asset, context));
        }
        byAssetId.add(assetId, byAssetFileId);
      }
      return byAssetId;
    }
  };

}
