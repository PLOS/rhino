package org.ambraproject.rhino.view.asset.groomed;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.view.JsonOutputView;

public class GroomedAssetFileView implements JsonOutputView {

  private final ArticleAsset asset;
  private final boolean includeFigureFields;

  private GroomedAssetFileView(ArticleAsset asset, boolean includeFigureFields) {
    this.asset = Preconditions.checkNotNull(asset);
    this.includeFigureFields = includeFigureFields;
  }

  /**
   * Create a view of the asset file, suppressing metadata fields that would be shown for a parent figure..
   */
  public static GroomedAssetFileView create(ArticleAsset asset) {
    return new GroomedAssetFileView(asset, false);
  }

  /**
   * Create a view of the asset file, showing all stored metadata.
   */
  public static GroomedAssetFileView createWithFullMetadata(ArticleAsset asset) {
    return new GroomedAssetFileView(asset, true);
  }

  @Override
  public JsonObject serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();

    if (AssetFileIdentity.hasFile(asset)) {
      AssetFileIdentity identity = AssetFileIdentity.from(asset);
      serialized.addProperty("file", identity.getFilePath());
    } // else, there is no file that goes with this uninitialized asset

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
