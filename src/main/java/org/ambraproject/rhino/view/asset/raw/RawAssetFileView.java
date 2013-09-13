package org.ambraproject.rhino.view.asset.raw;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.view.JsonOutputView;

/**
 * Wrapper for serializing the metadata of a single asset file object.
 * <p/>
 * For now, this wrapper produces the same serialization behavior as passing an unwrapped {@link ArticleAsset} object to
 * Gson. Nonetheless, any serialized {@link ArticleAsset} objects should be wrapped in this class, so it may act as a
 * hook for more complex serialization in case it is needed in the future.
 */
public class RawAssetFileView implements JsonOutputView {

  private final ArticleAsset asset;

  public RawAssetFileView(ArticleAsset asset) {
    this.asset = Preconditions.checkNotNull(asset);
  }

  @Override
  public JsonElement serialize(JsonSerializationContext context) {
    // For now, just use Gson's default object serialization
    return context.serialize(asset);
  }

}
