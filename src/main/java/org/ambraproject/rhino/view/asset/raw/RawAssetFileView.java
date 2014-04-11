package org.ambraproject.rhino.view.asset.raw;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.article.ArticleVisibility;

/**
 * Wrapper for serializing the metadata of a single asset file object.
 * <p/>
 * For now, this wrapper produces the same serialization behavior as passing an unwrapped {@link ArticleAsset} object to
 * Gson. Nonetheless, any serialized {@link ArticleAsset} objects should be wrapped in this class, so it may act as a
 * hook for more complex serialization in case it is needed in the future.
 */
public class RawAssetFileView implements JsonOutputView {

  private final ArticleAsset asset;
  private final ArticleVisibility parentArticle;

  public RawAssetFileView(ArticleAsset asset, ArticleVisibility parentArticle) {
    this.asset = Preconditions.checkNotNull(asset);
    this.parentArticle = Preconditions.checkNotNull(parentArticle);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    JsonObject serialized = context.serialize(asset).getAsJsonObject();
    serialized.add("parentArticle", context.serialize(parentArticle));
    return serialized;
  }

}
