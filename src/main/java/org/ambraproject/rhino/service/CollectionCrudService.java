package org.ambraproject.rhino.service;

import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.model.ArticleCollection;
import org.ambraproject.rhino.util.response.Transceiver;

import java.util.Collection;
import java.util.Set;

public interface CollectionCrudService {

  /**
   * Create a new collection.
   *
   * @param journalKey the key of the journal to which the collection will belong
   * @param slug       the slug to assign to the collection
   * @param title      the collection title
   * @param articleIds the non-empty set of articles to insert into the new collection
   * @return the created collection
   */
  ArticleCollection create(String journalKey, String slug, String title, Set<ArticleIdentity> articleIds);

  /**
   * Modify an existing collection.
   * <p/>
   * The first two arguments identify the collection to modify. The last two arguments represent new values to assign,
   * and a {@code null} value signifies that the value should not be modified. Note that a non-null value of {@code
   * articleIds} completely replaces the old collection of articles; omitting an article that is already in the
   * collection will remove it.
   *
   * @param journalKey the key of the journal to which the collection belongs
   * @param slug       the slug of the collection to modify
   * @param title      the new collection title, or {@code null} to leave the title unchanged
   * @param articleIds the new set of articles in the collection, or {@code null} to leave them unchanged
   * @return the modified collection
   */
  ArticleCollection update(String journalKey, String slug, String title, Set<ArticleIdentity> articleIds);

  Transceiver read(String journalKey, String slug);

  Collection<ArticleCollection> getContainingCollections(ArticleIdentity articleId);

}
