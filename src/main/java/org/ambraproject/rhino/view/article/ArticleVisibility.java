package org.ambraproject.rhino.view.article;

import com.google.common.base.Preconditions;
import org.ambraproject.models.Article;
import org.ambraproject.models.Journal;
import org.ambraproject.rhino.view.journal.JournalNonAssocView;

import java.util.Collection;

/**
 * An object describing an article's visibility: whether it is in a published state, and the set of journals in which it
 * has been published.
 */
public class ArticleVisibility {

  private final String doi;
  private final String state;
  private final JournalNonAssocView.ListView journals;

  public ArticleVisibility(String doi, int state, Collection<Journal> journals) {
    this.doi = Preconditions.checkNotNull(doi);
    this.state = ArticleJsonConstants.getPublicationStateName(state);
    this.journals = JournalNonAssocView.wrapList(journals);
    Preconditions.checkArgument(this.state != null);
  }

  public static ArticleVisibility create(Article article) {
    return new ArticleVisibility(article.getDoi(), article.getState(), article.getJournals());
  }
}
