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
  private final AssetIdentity id;

  /**
   * The asset file that represents the original (high-resolution) version of a figure.
   */
  private final ArticleAsset original;

  /**
   * The asset files that are meant to be thumbnail versions of the figure, in order by size.
   */
  private final ImmutableList<ArticleAsset> thumbnails;

  public Figure(AssetIdentity id, ArticleAsset original, List<ArticleAsset> thumbnails) {
    this.id = Preconditions.checkNotNull(id);
    this.original = Preconditions.checkNotNull(original);
    this.thumbnails = ImmutableList.copyOf(thumbnails);
  }

  public AssetIdentity getId() {
    return id;
  }

  public ArticleAsset getOriginal() {
    return original;
  }

  public ImmutableList<ArticleAsset> getThumbnails() {
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
    return listFigures(article.getAssets());
  }

  /**
   * Produce a list of the figures among a collection of assets.
   *
   * @param assets
   * @return
   */
  public static ImmutableList<Figure> listFigures(Collection<ArticleAsset> assets) {
    Preconditions.checkNotNull(assets);
    Map<AssetIdentity, ArticleAsset> originals = Maps.newLinkedHashMap();
    Multimap<AssetIdentity, ArticleAsset> thumbnails = LinkedListMultimap.create();
    for (ArticleAsset asset : assets) {
      AssetIdentity identity = AssetIdentity.from(asset);
      String extension = asset.getExtension();
      if (ORIGINAL_SUFFIX.equals(extension)) {
        ArticleAsset previous = originals.put(identity, asset);
        if (previous != null) {
          String message = String.format("Collision on original figures: %s; %s",
              previous.getDoi(), asset.getDoi());
          throw new RuntimeException(message);
        }
      } else if (THUMBNAIL_EXTENSIONS.contains(extension)) {
        thumbnails.put(identity, asset);
      }
    }

    List<Figure> figures = Lists.newArrayListWithCapacity(originals.size());
    for (Map.Entry<AssetIdentity, ArticleAsset> entry : originals.entrySet()) {
      ArticleAsset originalFigure = entry.getValue();
      Collection<ArticleAsset> figureThumbnails = thumbnails.get(entry.getKey());
      figures.add(create(originalFigure, figureThumbnails));
    }
    return ImmutableList.copyOf(figures);
  }

  private static Figure create(ArticleAsset original, Collection<ArticleAsset> thumbnails) {
    AssetIdentity figureId = AssetIdentity.from(original);
    List<ArticleAsset> sortedThumbnails = THUMBNAIL_ORDER.sortedCopy(thumbnails);
    return new Figure(figureId, original, sortedThumbnails);
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
