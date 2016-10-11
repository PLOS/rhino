package org.ambraproject.rhino.view;

import com.google.common.base.Preconditions;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.view.article.ArticleOverview;

import java.util.Objects;

public class ResolvedDoiView {

  public static enum DoiWorkType {
    ARTICLE(true), ASSET(true), VOLUME(false), ISSUE(false), COMMENT(false);

    private final boolean isVersioned;

    private DoiWorkType(boolean isVersioned) {
      this.isVersioned = isVersioned;
    }
  }

  private final String doi;
  private final String type;
  private final ArticleOverview article; // nullable

  private ResolvedDoiView(Doi doi, DoiWorkType type, ArticleOverview article) {
    this.doi = doi.getName();
    this.type = type.name().toLowerCase();
    this.article = article;
  }

  public static ResolvedDoiView create(Doi doi, DoiWorkType type) {
    Preconditions.checkArgument(!type.isVersioned);
    return new ResolvedDoiView(doi, type, null);
  }

  public static ResolvedDoiView createForArticle(Doi doi, DoiWorkType type, ArticleOverview article) {
    Preconditions.checkArgument(type.isVersioned);
    return new ResolvedDoiView(doi, type, Objects.requireNonNull(article));
  }

}
