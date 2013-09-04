package org.ambraproject.rhino.view.asset.groomed;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.view.JsonOutputView;

public class AssetFileView implements JsonOutputView {

  private final ArticleAsset asset;

  public AssetFileView(ArticleAsset asset) {
    this.asset = Preconditions.checkNotNull(asset);
  }

  @Override
  public JsonObject serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    AssetFileIdentity identity = AssetFileIdentity.from(asset);
    serialized.addProperty("file", identity.getFilePath());
    serialized.add("metadata", context.serialize(asset));
    return serialized;
  }

}
