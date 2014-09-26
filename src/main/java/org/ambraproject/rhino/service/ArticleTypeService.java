package org.ambraproject.rhino.service;

import org.ambraproject.models.Article;

import java.net.URI;

/**
 * Access data about article types.
 */
public interface ArticleTypeService {

  /**
   * Get metadata associated with an article type URI. The metadata is a constant object stored by the system.
   *
   * @param uri a URI identifying an article type
   * @return the metadata for the identified article type, or {@code null} if none is stored in the system
   */
  public abstract ArticleType getMetadataForUri(URI uri);

  /**
   * Get metadata for the type associated with an article, or {@code null} if it has no type.
   * <p/>
   * This is distinct from {@link Article#getTypes()}, which returns a set of URIs. This method encapsulates logic for
   * producing a <em>single</em> type object that represents the article.
   *
   * @param article an article
   * @return the article's type, or {@code null} if it has none
   */
  public abstract ArticleType getFor(Article article);

}
