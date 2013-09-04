package org.ambraproject.rhino.view.asset.groomed;


import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.Collection;
import java.util.Map;

public class GroomedFigureView implements JsonOutputView {

  public static enum FigureType {
    ORIGINAL("TIF"),
    SMALL("PNG_S"),
    INLINE("PNG_I"),
    MEDIUM("PNG_M"),
    LARGE("PNG_L");

    private final ImmutableSet<String> associatedExtensions;

    private FigureType(String... associatedExtensions) {
      this.associatedExtensions = ImmutableSet.copyOf(associatedExtensions);
    }

    private static final ImmutableMap<String, FigureType> TYPES_BY_EXTENSION;

    static {
      ImmutableMap.Builder<String, FigureType> builder = ImmutableMap.builder();
      for (FigureType figureType : values()) {
        for (String extension : figureType.associatedExtensions) {
          builder.put(extension.toUpperCase(), figureType);
        }
      }
      TYPES_BY_EXTENSION = builder.build();
    }

    static ImmutableSet<String> getExtensions() {
      return TYPES_BY_EXTENSION.keySet();
    }

    static FigureType fromExtension(String extension) {
      FigureType figureType = TYPES_BY_EXTENSION.get(extension.toUpperCase());
      if (figureType == null) {
        String message = String.format("Figure extension not matched: \"%s\". Expected one of: %s",
            extension, getExtensions());
        throw new IllegalArgumentException(message);
      }
      return figureType;
    }
  }


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
          + FigureType.ORIGINAL.associatedExtensions;
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
