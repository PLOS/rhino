package org.ambraproject.rhino.view.asset.groomed;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.view.JsonOutputView;

public class AssetFileView implements JsonOutputView {

  private final ArticleAsset asset;
  private final boolean includeFigureFields;

  private AssetFileView(ArticleAsset asset, boolean includeFigureFields) {
    this.asset = Preconditions.checkNotNull(asset);
    this.includeFigureFields = includeFigureFields;
  }

  /**
   * Create a view of the asset file, suppressing
   */
  public static AssetFileView create(ArticleAsset asset) {
    return new AssetFileView(asset, false);
  }

  public static AssetFileView createWithFullMetadata(ArticleAsset asset) {
    return new AssetFileView(asset, true);
  }

  @Override
  public JsonObject serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    AssetFileIdentity identity = AssetFileIdentity.from(asset);
    serialized.addProperty("file", identity.getFilePath());

    JsonObject serializedMetadata = (JsonObject) context.serialize(asset);
    if (!includeFigureFields) {
      for (FigureMetadataField field : FigureMetadataField.values()) {
        serializedMetadata.remove(field.getMemberName());
      }
    }
    serialized.add("metadata", serializedMetadata);

    return serialized;
  }

}
