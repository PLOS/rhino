package org.ambraproject.rhino.service;

import com.google.common.base.Optional;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.ArticleLinkIdentity;
import org.ambraproject.rhino.model.ArticleLink;
import org.ambraproject.rhino.util.response.Transceiver;

import java.util.Collection;
import java.util.Set;

public interface ArticleLinkCrudService {

  /**
   * Add a new link to a set of articles.
   *
   * @param identity   the identity of the link to create
   * @param title      the link text
   * @param articleIds the non-empty set of articles that will display the link
   * @return the created collection
   */
  ArticleLink create(ArticleLinkIdentity identity, String title, Set<ArticleIdentity> articleIds);

  /**
   * Modify the set of articles that display a link.
   * <p/>
   * The first two arguments identify the set to modify. The last two arguments represent new values to assign, and an
   * absent value signifies that the value should not be modified. Note that a present value of {@code articleIds}
   * completely replaces the old collection of articles; omitting an article that is already in the collection will
   * remove it.
   *
   * @param identity   the identity of the link to update
   * @param title      the new link text, or {@code null} to leave the title unchanged
   * @param articleIds the new set of articles to display the link, or {@code null} to leave them unchanged
   * @return the modified collection
   */
  ArticleLink update(ArticleLinkIdentity identity,
                     Optional<String> title, Optional<? extends Set<ArticleIdentity>> articleIds);

  Transceiver read(ArticleLinkIdentity identity);

  Collection<ArticleLink> getAssociatedLinks(ArticleIdentity articleId);

}
