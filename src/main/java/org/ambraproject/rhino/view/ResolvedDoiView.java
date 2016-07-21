package org.ambraproject.rhino.view;

import com.google.common.base.Preconditions;
import org.ambraproject.rhino.view.article.versioned.ArticleOverview;

import java.util.Objects;

public class ResolvedDoiView {

  public static enum DoiWorkType {
    ARTICLE(true), ASSET(true), VOLUME(false), ISSUE(false), COMMENT(false);

    private final boolean isVersioned;

    private DoiWorkType(boolean isVersioned) {
      this.isVersioned = isVersioned;
    }
  }

  private final String type;
  private final ArticleOverview article; // nullable

  private ResolvedDoiView(DoiWorkType type, ArticleOverview article) {
    this.type = type.name().toLowerCase();
    this.article = article;
  }

  public static ResolvedDoiView create(DoiWorkType type) {
    Preconditions.checkArgument(!type.isVersioned);
    return new ResolvedDoiView(type, null);
  }

  public static ResolvedDoiView createForArticle(DoiWorkType type, ArticleOverview article) {
    Preconditions.checkArgument(type.isVersioned);
    return new ResolvedDoiView(type, Objects.requireNonNull(article));
  }

}
