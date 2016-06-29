package org.ambraproject.rhino.view.article;

import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.view.KeyedStringList;

import java.util.Collection;

/**
 * A list of bare DOIs that can be serialized to JSON with structure identical to {@link ArticleViewList}.
 */
public class DoiList extends KeyedStringList {

  public DoiList(Collection<String> dois) {
    super(dois);
  }

  @Override
  protected String extractIdentifier(String value) {
    return DoiBasedIdentity.asIdentifier(value);
  }

  @Override
  protected String getMemberName() {
    return ArticleJsonNames.DOI;
  }

}
