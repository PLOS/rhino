package org.ambraproject.rhino.view;

import com.google.common.base.Preconditions;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.view.article.ArticleOverview;
import org.ambraproject.rhino.view.journal.JournalOutputView;

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
  private final JournalOutputView journal; // nullable
  private final ArticleOverview article; // nullable

  private ResolvedDoiView(Doi doi, DoiWorkType type, JournalOutputView journal, ArticleOverview article) {
    Preconditions.checkArgument((journal == null) != (article == null));
    this.doi = doi.getName();
    this.type = type.name().toLowerCase();
    this.journal = journal;
    this.article = article;
  }

  public static ResolvedDoiView create(Doi doi, DoiWorkType type, Journal journal) {
    Preconditions.checkArgument(!type.isVersioned);
    return new ResolvedDoiView(doi, type, JournalOutputView.getView(journal), null);
  }

  public static ResolvedDoiView createForArticle(Doi doi, DoiWorkType type, ArticleOverview article) {
    Preconditions.checkArgument(type.isVersioned);
    return new ResolvedDoiView(doi, type, null, Objects.requireNonNull(article));
  }

}
