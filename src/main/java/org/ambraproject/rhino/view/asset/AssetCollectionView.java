package org.ambraproject.rhino.view.asset;

import com.google.common.collect.ImmutableListMultimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AssetCollectionView implements JsonOutputView {

  private final ImmutableListMultimap<String, ArticleAsset> assets;

  public AssetCollectionView(Iterable<ArticleAsset> assets) {
    ImmutableListMultimap.Builder<String, ArticleAsset> buffer = ImmutableListMultimap.builder();
    for (ArticleAsset asset : assets) {
      buffer.put(asset.getDoi(), asset);
    }
    this.assets = buffer.build();
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject byAssetId = new JsonObject();
    for (Map.Entry<String, Collection<ArticleAsset>> entry : assets.asMap().entrySet()) {
      String assetId = DoiBasedIdentity.removeScheme(entry.getKey());
      List<ArticleAsset> assetFiles = (List<ArticleAsset>) entry.getValue(); // cast is safe because it's a ListMultimap
      JsonElement byFileId = AssetFileCollectionView.serializeAssetFiles(assetFiles, context);
      byAssetId.add(assetId, byFileId);
    }
    return byAssetId;
  }

}
