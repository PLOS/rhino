package org.ambraproject.rhino.view.asset.raw;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.view.JsonOutputView;

public class AssetsAsFigureView implements JsonOutputView {

  private final Figure figure;
  private final ArticleIdentity parentArticleIdentity;

  public AssetsAsFigureView(Figure figure, ArticleIdentity parentArticleIdentity) {
    this.figure = Preconditions.checkNotNull(figure);
    this.parentArticleIdentity = Preconditions.checkNotNull(parentArticleIdentity);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    final ArticleAsset original = figure.getOriginal();

    JsonObject serialized = new JsonObject();
    serialized.addProperty("doi", figure.getId().getIdentifier());
    serialized.addProperty("parentArticleId", parentArticleIdentity.getIdentifier());

    serialized.add("original", context.serialize(original));
    serialized.add("thumbnails", context.serialize(figure.getThumbnails()));

    return serialized;
  }

}
