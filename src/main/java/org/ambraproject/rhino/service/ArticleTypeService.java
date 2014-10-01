package org.ambraproject.rhino.service;

import org.ambraproject.models.Article;

/**
 * Access data about article types.
 */
public interface ArticleTypeService {

  /**
   * Get the article type as defined by the NLM DTD. This is the value given in XML as the {@code article-type}
   * attribute of the root {@code &lt;article>} element. It must be one of the values from {@link
   * org.ambraproject.rhino.util.NlmArticleTypes#TYPES}.
   *
   * @param article an article object
   * @return its article type per the NLM DTD, or {@code null} if the article has none
   */
  public abstract String getNlmArticleType(Article article);

  /**
   * Get the article type as defined by our system.
   *
   * @param article the article
   * @return the article's type, or {@code null} if the article has none
   */
  public abstract ArticleType getArticleType(Article article);

}
