package org.ambraproject.rhino.content.view;

import com.google.common.collect.ImmutableListMultimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.ArticleIdentity;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
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

  public static final JsonSerializer<AssetCollectionView> SERIALIZER = new JsonSerializer<AssetCollectionView>() {
    @Override
    public JsonElement serialize(AssetCollectionView src, Type typeOfSrc, JsonSerializationContext context) {
      JsonObject byAssetId = new JsonObject();
      for (Map.Entry<String, Collection<ArticleAsset>> entry : src.assets.asMap().entrySet()) {
        String assetId = ArticleIdentity.removeScheme(entry.getKey());
        List<ArticleAsset> assetFiles = (List<ArticleAsset>) entry.getValue(); // cast is safe because it's a ListMultimap
        JsonElement byFileId = AssetFileCollectionView.serializeAssetFiles(assetFiles, context);
        byAssetId.add(assetId, byFileId);
      }
      return byAssetId;
    }
  };

}
