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
  private final Map<FigureType, ArticleAsset> thumbnails;

  private GroomedFigureView(ArticleAsset original, Map<FigureType, ArticleAsset> thumbnails) {
    this.original = Preconditions.checkNotNull(original);
    this.thumbnails = ImmutableSortedMap.copyOf(thumbnails);
    Preconditions.checkArgument(!this.thumbnails.containsKey(FigureType.ORIGINAL));
  }

  public static GroomedFigureView create(Collection<ArticleAsset> figureAssets) {
    Map<FigureType, ArticleAsset> byType = Maps.newEnumMap(FigureType.class);
    for (ArticleAsset asset : figureAssets) {
      byType.put(FigureType.fromExtension(asset.getExtension()), asset);
    }

    ArticleAsset original = byType.remove(FigureType.ORIGINAL);
    if (original == null) {
      String message = "Original asset not found. Expected an asset with an extension: "
          + FigureType.ORIGINAL.getAssociatedExtensions();
      throw new IllegalArgumentException(message);
    }

    return new GroomedFigureView(original, byType);
  }

  @Override
  public JsonObject serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();

    serialized.addProperty("doi", original.getDoi());
    serialized.addProperty("title", original.getTitle());
    serialized.addProperty("description", original.getDescription());

    serialized.add("original", context.serialize(new AssetFileView(original)));

    JsonObject serializedThumbnails = new JsonObject();
    for (Map.Entry<FigureType, ArticleAsset> entry : thumbnails.entrySet()) {
      String key = entry.getKey().name().toLowerCase();
      ArticleAsset thumbnail = entry.getValue();
      serializedThumbnails.add(key, context.serialize(new AssetFileView(thumbnail)));
    }
    serialized.add("thumbnails", serializedThumbnails);

    return serialized;
  }

}
