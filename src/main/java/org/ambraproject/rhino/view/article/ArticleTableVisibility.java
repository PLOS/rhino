package org.ambraproject.rhino.view.article;

import org.ambraproject.rhino.identity.Doi;

import java.util.Objects;

/**
 * An object describing an article's visibility: whether it is in a published state, and the set of journals in which it
 * has been published.
 */
//todo: merge this class with ArticleVisibility once all service classes are updated to new model
public class ArticleTableVisibility {

  private final Doi doi;

  private ArticleTableVisibility(Doi doi) {
    this.doi = Objects.requireNonNull(doi);
  }

  public static ArticleTableVisibility create(Doi doi) {
    return new ArticleTableVisibility(doi);
  }
}
