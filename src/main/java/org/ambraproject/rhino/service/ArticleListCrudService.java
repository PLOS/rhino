package org.ambraproject.rhino.service;

import com.google.common.base.Optional;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleListIdentity;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.view.journal.ArticleListView;

import java.util.Set;

public interface ArticleListCrudService {

  /**
   * Create a new article list.
   *
   * @param identity    the identity of the list to create
   * @param displayName the list's display name
   * @param articleIds  the non-empty set of articles that will display the link
   * @return the created list (augmented with its parent journal key)
   */
  ArticleListView create(ArticleListIdentity identity, String displayName, Set<ArticleIdentifier> articleIds);

  ArticleListView update(ArticleListIdentity identity, Optional<String> displayName,
                         Optional<? extends Set<ArticleIdentifier>> articleIds);

  ServiceResponse read(ArticleListIdentity identity);

  ServiceResponse readAll(Optional<String> listType, Optional<String> journalKey);

  /**
   * Read all lists that contain the article.
   *
   * @param articleId the identity of an article
   * @return the identities of all lists that contain the article
   */
  ServiceResponse readContainingLists(ArticleIdentifier articleId);

}
