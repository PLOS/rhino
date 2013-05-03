package org.ambraproject.rhino.view.article;

import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.view.KeyedListView;

import java.util.Collection;

/**
 * A list of article views that are keyed by DOI. The list should be serialized as a JSON object: the members are the
 * objects in the list, and the member names are the objects' REST IDs. An object's REST ID is the same as its DOI
 * without the {@code "info:doi/"} prefix.
 */
public class ArticleViewList extends KeyedListView<ArticleView> {

  public ArticleViewList(Collection<? extends ArticleView> values) {
    super(values);
  }

  @Override
  protected String getKey(ArticleView value) {
    return ArticleIdentity.removeScheme(value.getDoi());
  }

}
