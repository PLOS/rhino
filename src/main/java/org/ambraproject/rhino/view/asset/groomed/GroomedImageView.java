package org.ambraproject.rhino.view.asset.groomed;


import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.view.JsonOutputView;

import java.util.Collection;
import java.util.Map;

public class GroomedImageView implements JsonOutputView {

  enum ImageType {FIGURE, GRAPHIC}

  private final ArticleAsset original;
  private final Map<ImageFileType, ArticleAsset> thumbnails;
  private Optional<ArticleIdentity> parentArticleId;
  private final ImageType imageType;

  private GroomedImageView(ArticleAsset original,
                           Map<ImageFileType, ArticleAsset> thumbnails,
                           ImageType imageType) {
    this.original = Preconditions.checkNotNull(original);
    this.thumbnails = ImmutableSortedMap.copyOf(thumbnails);
    Preconditions.checkArgument(!this.thumbnails.isEmpty());
    Preconditions.checkArgument(!this.thumbnails.containsKey(ImageFileType.ORIGINAL));
    this.parentArticleId = Optional.absent();
    this.imageType = Preconditions.checkNotNull(imageType);
  }


  private static final ImmutableSet<ImageFileType> FIGURE_TYPES = Sets.immutableEnumSet(
      ImageFileType.SMALL, ImageFileType.INLINE, ImageFileType.MEDIUM, ImageFileType.LARGE);
  private static final ImmutableSet<ImageFileType> GRAPHIC_TYPES = Sets.immutableEnumSet(ImageFileType.GRAPHIC);

  // These exceptions are routinely caught (for "miscellaneous" assets -- see GroomedAssetsView),
  // so it's worth it not to build these strings from sets over and over
  private static final String ORIGINAL_NOT_FOUND_MESSAGE = "Original asset not found. Expected an asset with an extension: "
      + ImageFileType.ORIGINAL.getAssociatedExtensions();
  private static final String THUMBNAIL_NOT_FOUND_MESSAGE = "Thumbnails not found. Expected an asset with an extension: "
      + Sets.difference(ImageFileType.getAllExtensions(), ImageFileType.ORIGINAL.getAssociatedExtensions());
  private static final String UNCATEGORIZED_EXCEPTION = String.format(
      "Failed to categorize asset as an image. Expected thumbnails to be either %s (figure) or %s (graphic).",
      FIGURE_TYPES, GRAPHIC_TYPES);

  public static GroomedImageView create(Collection<ArticleAsset> figureAssets) {
    Map<ImageFileType, ArticleAsset> byType = Maps.newEnumMap(ImageFileType.class);
    for (ArticleAsset asset : figureAssets) {
      byType.put(ImageFileType.fromExtension(asset.getExtension()), asset);
    }

    ArticleAsset original = byType.remove(ImageFileType.ORIGINAL);
    if (original == null) {
      throw new UncategorizedAssetException(ORIGINAL_NOT_FOUND_MESSAGE);
    }
    if (byType.isEmpty()) {
      throw new UncategorizedAssetException(THUMBNAIL_NOT_FOUND_MESSAGE);
    }

    ImageType imageType;
    if (GRAPHIC_TYPES.equals(byType.keySet())) {
      imageType = ImageType.GRAPHIC;
    } else if (FIGURE_TYPES.equals(byType.keySet())) {
      imageType = ImageType.FIGURE;
    } else {
      throw new UncategorizedAssetException(UNCATEGORIZED_EXCEPTION);
    }

    return new GroomedImageView(original, byType, imageType);
  }

  public AssetIdentity getIdentity() {
    return AssetIdentity.from(original);
  }

  ImageType getImageType() {
    return imageType;
  }

  /**
   * Add a parent article ID to the view.
   *
   * @param parentArticleId the article ID to add as the figure's parent
   * @return this object
   * @throws IllegalStateException if a parent article has already been set
   * @throws NullPointerException  if {@code parentArticleId} is null
   */
  public GroomedImageView setParentArticle(ArticleIdentity parentArticleId) {
    Preconditions.checkState(!this.parentArticleId.isPresent());
    this.parentArticleId = Optional.of(parentArticleId);
    return this;
  }

  @Override
  public JsonObject serialize(JsonSerializationContext context) {
    JsonObject serialized = new JsonObject();
    serialized.addProperty("doi", original.getDoi());

    if (parentArticleId.isPresent()) {
      serialized.addProperty("parentArticleId", parentArticleId.get().getIdentifier());
    }

    // Pull figure-level metadata values from the original (ignore those of thumbnails)
    for (ImageMetadataField field : ImageMetadataField.values()) {
      serialized.add(field.getMemberName(), context.serialize(field.access(original)));
    }

    serialized.add("original", context.serialize(GroomedAssetFileView.create(original)));

    JsonObject serializedThumbnails = new JsonObject();
    for (Map.Entry<ImageFileType, ArticleAsset> entry : thumbnails.entrySet()) {
      String key = entry.getKey().name().toLowerCase();
      GroomedAssetFileView thumbnailView = GroomedAssetFileView.create(entry.getValue());
      serializedThumbnails.add(key, context.serialize(thumbnailView));
    }
    serialized.add("thumbnails", serializedThumbnails);

    return serialized;
  }

}
