package org.ambraproject.rhino.view.asset;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.Collection;

public class AssetsAsFigureView implements JsonOutputView {

  private final Figure figure;
  private final ArticleIdentity parentArticleIdentity;

  public AssetsAsFigureView(Figure figure, ArticleIdentity parentArticleIdentity) {
    this.figure = Preconditions.checkNotNull(figure);
    this.parentArticleIdentity = Preconditions.checkNotNull(parentArticleIdentity);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    AssetIdentity assetId = figure.getId();
    ArticleAsset original = figure.getOriginal();

    JsonObject serialized = new JsonObject();
    serialized.addProperty("doi", assetId.getIdentifier());
    serialized.addProperty("parentArticleId", parentArticleIdentity.getIdentifier());
    serialized.addProperty("contextElement", original.getContextElement());
    serialized.addProperty("title", original.getTitle());
    serialized.addProperty("description", original.getDescription());

    Collection<ArticleAsset> assets = figure.getAllAssets();
    serialized.add("assetfiles", context.serialize(new AssetFileCollectionView(assets)));

    return serialized;
  }

}
