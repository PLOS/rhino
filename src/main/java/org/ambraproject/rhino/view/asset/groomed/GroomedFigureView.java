package org.ambraproject.rhino.view.asset.groomed;


import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.Collection;
import java.util.Map;

public class GroomedFigureView implements JsonOutputView {

  private final ArticleAsset original;
  private final Map<FigureFileType, ArticleAsset> thumbnails;

  private GroomedFigureView(ArticleAsset original, Map<FigureFileType, ArticleAsset> thumbnails) {
    this.original = Preconditions.checkNotNull(original);
    this.thumbnails = ImmutableSortedMap.copyOf(thumbnails);
    Preconditions.checkArgument(!this.thumbnails.containsKey(FigureFileType.ORIGINAL));
  }

  public static GroomedFigureView create(Collection<ArticleAsset> figureAssets) {
    Map<FigureFileType, ArticleAsset> byType = Maps.newEnumMap(FigureFileType.class);
    for (ArticleAsset asset : figureAssets) {
      byType.put(FigureFileType.fromExtension(asset.getExtension()), asset);
    }

    ArticleAsset original = byType.remove(FigureFileType.ORIGINAL);
    if (original == null) {
      String message = "Original asset not found. Expected an asset with an extension: "
          + FigureFileType.ORIGINAL.getAssociatedExtensions();
      throw new IllegalArgumentException(message);
    }

    return new GroomedFigureView(original, byType);
  }

  @Override
  public JsonObject serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("doi", original.getDoi());

    // Pull figure-level metadata values from the original (ignore those of thumbnails)
    for (FigureMetadataField field : FigureMetadataField.values()) {
      serialized.add(field.getMemberName(), context.serialize(field.access(original)));
    }

    serialized.add("original", context.serialize(GroomedAssetFileView.create(original)));

    JsonObject serializedThumbnails = new JsonObject();
    for (Map.Entry<FigureFileType, ArticleAsset> entry : thumbnails.entrySet()) {
      String key = entry.getKey().name().toLowerCase();
      GroomedAssetFileView thumbnailView = GroomedAssetFileView.create(entry.getValue());
      serializedThumbnails.add(key, context.serialize(thumbnailView));
    }
    serialized.add("thumbnails", serializedThumbnails);

    return serialized;
  }

}
