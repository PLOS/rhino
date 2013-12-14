package org.ambraproject.rhino.view.asset.groomed;


import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
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

  private final ArticleAsset original;
  private final Map<ImageFileType, ArticleAsset> thumbnails;
  private Optional<ArticleIdentity> parentArticleId;

  private GroomedImageView(ArticleAsset original,
                           Map<ImageFileType, ArticleAsset> thumbnails) {
    this.original = Preconditions.checkNotNull(original);
    this.thumbnails = ImmutableSortedMap.copyOf(thumbnails);
    Preconditions.checkArgument(!this.thumbnails.isEmpty());
    Preconditions.checkArgument(!this.thumbnails.containsKey(ImageFileType.ORIGINAL));
    this.parentArticleId = Optional.absent();
  }


  // These exceptions are routinely caught (for "miscellaneous" assets -- see GroomedAssetsView),
  // so it's worth it not to build these strings from sets over and over
  private static final String ORIGINAL_NOT_FOUND_MESSAGE = "Original asset not found. Expected an asset with an extension: "
      + ImageFileType.ORIGINAL.getAssociatedExtensions();
  private static final String THUMBNAIL_NOT_FOUND_MESSAGE = "Thumbnails not found. Expected an asset with an extension: "
      + Sets.difference(ImageFileType.getAllExtensions(), ImageFileType.ORIGINAL.getAssociatedExtensions());

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

    return new GroomedImageView(original, byType);
  }

  public AssetIdentity getIdentity() {
    return AssetIdentity.from(original);
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
