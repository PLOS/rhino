package org.ambraproject.rhino.view.journal;

import org.ambraproject.models.Issue;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.view.KeyedListView;

import java.util.Collection;

public class IssueListView extends KeyedListView<Issue> {

  public IssueListView(Collection<? extends Issue> values) {
    super(values);
  }

  @Override
  protected String getKey(Issue value) {
    return ArticleIdentity.removeScheme(value.getIssueUri());
  }

}
