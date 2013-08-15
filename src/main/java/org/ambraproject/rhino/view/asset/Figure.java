package org.ambraproject.rhino.view.asset;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A representation of an article's figures.
 * <p/>
 * Figures are a subset of assets. In Ambra's legacy data model, whether an asset is a figure is determined entirely by
 * its file extension. This logic continues to be hard-coded for now, the idea being to tuck it away underneath the API
 * until something more flexible can be implemented.
 * <p/>
 * The purpose of the Figure view is to provide an explicit list of those assets which are article figures, in order. A
 * client can't get this information just from looking at the "assets" property for two reasons: it is an unordered
 * dictionary; and they would have to infer which assets are figures by inspecting the extensions, which we don't want
 * the client to do.
 */
public class Figure {

  /**
   * The ID of an asset (i.e., group of asset files) that represents a figure.
   */
  private final String id;

  /**
   * The ID of the asset file that represents the original (high-resolution) version of a figure.
   */
  private final String original;

  /**
   * The IDs of asset files that are meant to be thumbnail versions of the figure.
   */
  private final ImmutableList<String> thumbnails;

  private Figure(String id, String original, List<String> thumbnails) {
    this.id = Preconditions.checkNotNull(id);
    this.original = Preconditions.checkNotNull(original);
    this.thumbnails = ImmutableList.copyOf(thumbnails);
  }

  public String getId() {
    return id;
  }

  public String getOriginal() {
    return original;
  }

  public ImmutableList<String> getThumbnails() {
    return thumbnails;
  }


  // Hard-coded values replicating legacy, extension-based figure logic
  private static final String ORIGINAL_SUFFIX = "TIF";
  private static final ImmutableSet<String> THUMBNAIL_EXTENSIONS = ImmutableSet.of("PNG_S", "PNG_M", "PNG_I", "PNG_L");
  private static final Ordering<ArticleAsset> THUMBNAIL_ORDER = Ordering
      .explicit(THUMBNAIL_EXTENSIONS.asList())
      .onResultOf(new Function<ArticleAsset, String>() {
        @Override
        public String apply(ArticleAsset input) {
          return input.getExtension().toUpperCase();
        }
      });

  /**
   * Produce a list of the figures among an article's assets.
   *
   * @param article
   * @return
   */
  public static ImmutableList<Figure> listFigures(Article article) {
    Preconditions.checkNotNull(article);
    Map<String, ArticleAsset> originals = Maps.newLinkedHashMap();
    Multimap<String, ArticleAsset> thumbnails = LinkedListMultimap.create();
    for (ArticleAsset asset : article.getAssets()) {
      String extension = asset.getExtension();
      if (ORIGINAL_SUFFIX.equals(extension)) {
        ArticleAsset previous = originals.put(asset.getDoi(), asset);
        if (previous != null) {
          String message = String.format("Collision on original figures: %s; %s",
              previous.getDoi(), asset.getDoi());
          throw new RuntimeException(message);
        }
      } else if (THUMBNAIL_EXTENSIONS.contains(extension)) {
        thumbnails.put(asset.getDoi(), asset);
      }
    }

    List<Figure> figures = Lists.newArrayListWithCapacity(originals.size());
    for (Map.Entry<String, ArticleAsset> entry : originals.entrySet()) {
      String id = entry.getKey();
      ArticleAsset originalFigure = entry.getValue();
      Collection<ArticleAsset> figureThumbnails = thumbnails.get(id);
      figures.add(create(originalFigure, figureThumbnails));
    }
    return ImmutableList.copyOf(figures);
  }

  private static Figure create(ArticleAsset original, Collection<ArticleAsset> thumbnails) {
    String figureId = AssetIdentity.from(original).getIdentifier();
    String originalFileId = AssetFileIdentity.from(original).getFilePath();
    List<String> thumbnailFileIds = Lists.newArrayListWithCapacity(thumbnails.size());
    List<ArticleAsset> sortedThumbnails = THUMBNAIL_ORDER.sortedCopy(thumbnails);
    for (ArticleAsset thumbnail : sortedThumbnails) {
      thumbnailFileIds.add(AssetFileIdentity.from(thumbnail).getFilePath());
    }
    return new Figure(figureId, originalFileId, thumbnailFileIds);
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Figure that = (Figure) o;

    if (!original.equals(that.original)) return false;
    if (!id.equals(that.id)) return false;
    if (!thumbnails.equals(that.thumbnails)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + original.hashCode();
    result = 31 * result + thumbnails.hashCode();
    return result;
  }
}
