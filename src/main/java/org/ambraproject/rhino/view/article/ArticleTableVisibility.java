package org.ambraproject.rhino.view.article;

import com.google.common.base.Preconditions;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.PublicationState;
import org.ambraproject.rhino.view.journal.JournalNonAssocView;

import java.util.Collection;
import java.util.Collections;

/**
 * An object describing an article's visibility: whether it is in a published state, and the set of journals in which it
 * has been published.
 */
//todo: merge this class with ArticleVisibility once all service classes are updated to new model
public class ArticleTableVisibility {

  private final Doi doi;
  private final String state;
  private final JournalNonAssocView.ListView journals;

  public ArticleTableVisibility(Doi doi, int state, Collection<Journal> journals) {
    this.doi = Preconditions.checkNotNull(doi);
    this.state = PublicationState.fromValue(state).getLabel();
    this.journals = JournalNonAssocView.wrapList(journals);
    Preconditions.checkArgument(this.state != null);
  }

  public static ArticleTableVisibility create(Doi doi) {
    return new ArticleTableVisibility(doi,
        1 /*todo: article state*/,
        Collections.EMPTY_SET /*todo: list of journals on article */);
  }
}
