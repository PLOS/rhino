package org.ambraproject.rhino.view.asset.raw;

import com.google.common.collect.ImmutableListMultimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.article.ArticleVisibility;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RawAssetCollectionView implements JsonOutputView {

  private final ImmutableListMultimap<String, ArticleAsset> assets;
  private final ArticleVisibility parentArticle;

  public RawAssetCollectionView(Article article) {
    this.parentArticle = ArticleVisibility.create(article);

    Iterable<ArticleAsset> assets = article.getAssets();
    ImmutableListMultimap.Builder<String, ArticleAsset> buffer = ImmutableListMultimap.builder();
    for (ArticleAsset asset : assets) {
      buffer.put(asset.getDoi(), asset);
    }
    this.assets = buffer.build();
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    for (Map.Entry<String, Collection<ArticleAsset>> entry : assets.asMap().entrySet()) {
      String assetId = DoiBasedIdentity.asIdentifier(entry.getKey());
      List<ArticleAsset> assetFiles = (List<ArticleAsset>) entry.getValue(); // cast is safe because it's a ListMultimap
      JsonElement byFileId = RawAssetFileCollectionView.serializeAssetFiles(assetFiles, parentArticle, context);
      serialized.add(assetId, byFileId);
    }
    return serialized;
  }
}
