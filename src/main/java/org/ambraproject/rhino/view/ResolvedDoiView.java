package org.ambraproject.rhino.view;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.view.article.ArticleOverview;
import org.ambraproject.rhino.view.journal.JournalOutputView;

import java.util.Objects;

public class ResolvedDoiView {

  public static enum DoiWorkType {
    ARTICLE, ASSET, VOLUME, ISSUE, COMMENT;
  }

  private static final ImmutableSet<DoiWorkType> BELONGS_TO_ARTICLE = Sets.immutableEnumSet(
      DoiWorkType.ARTICLE, DoiWorkType.ASSET, DoiWorkType.COMMENT);

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
    Preconditions.checkArgument(!BELONGS_TO_ARTICLE.contains(type));
    return new ResolvedDoiView(doi, type, JournalOutputView.getView(journal), null);
  }

  public static ResolvedDoiView createForArticle(Doi doi, DoiWorkType type, ArticleOverview article) {
    Preconditions.checkArgument(BELONGS_TO_ARTICLE.contains(type));
    return new ResolvedDoiView(doi, type, null, Objects.requireNonNull(article));
  }

}
