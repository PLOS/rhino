package org.ambraproject.rhino.service;

import org.ambraproject.models.ArticleList;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.ArticleListIdentity;
import org.ambraproject.rhino.util.response.Transceiver;

import java.util.Collection;
import java.util.Set;

public interface ArticleListCrudService {

  /**
   * Create a new article list.
   *
   * @param identity    the identity of the list to create
   * @param displayName the list's display name
   * @param articleIds  the non-empty set of articles that will display the link
   * @return the created collection
   */
  ArticleList create(ArticleListIdentity identity, String displayName, Set<ArticleIdentity> articleIds);

  ArticleList update(ArticleListIdentity identity, String displayName, Set<ArticleIdentity> articleIds);

  Transceiver read(ArticleListIdentity identity);

  Collection<ArticleList> getContainingLists(ArticleIdentity articleId);

}
