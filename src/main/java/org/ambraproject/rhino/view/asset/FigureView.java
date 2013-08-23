package org.ambraproject.rhino.view.asset;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.AssetFileIdentity;

import java.util.List;

public class FigureView {

  private static final Function<ArticleAsset, String> ASSET_TO_FILE_PATH = new Function<ArticleAsset, String>() {
    @Override
    public String apply(ArticleAsset input) {
      return AssetFileIdentity.from(input).getFilePath();
    }
  };

  private final String id;
  private final String original;
  private final List<String> thumbnails;

  public FigureView(Figure figure) {
    this.id = figure.getId().getIdentifier();
    this.original = ASSET_TO_FILE_PATH.apply(figure.getOriginal());
    this.thumbnails = Lists.transform(figure.getThumbnails(), ASSET_TO_FILE_PATH);
  }

  public static List<FigureView> asViewList(List<Figure> figures) {
    List<FigureView> views = Lists.newArrayListWithCapacity(figures.size());
    for (Figure figure : figures) {
      views.add(new FigureView(figure));
    }
    return views;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FigureView that = (FigureView) o;

    if (!id.equals(that.id)) return false;
    if (!original.equals(that.original)) return false;
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

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("id", id)
        .add("original", original)
        .add("thumbnails", thumbnails)
        .toString();
  }

}
