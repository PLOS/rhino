package org.ambraproject.rhino.view.article;

import org.ambraproject.rhino.identity.Doi;

import java.util.Objects;

/**
 * An object describing an article's visibility: whether it is in a published state, and the set of journals in which it
 * has been published.
 */
public class ArticleVisibility {

  private final Doi doi;

  private ArticleVisibility(Doi doi) {
    this.doi = Objects.requireNonNull(doi);
  }

  public static ArticleVisibility create(Doi doi) {
    return new ArticleVisibility(doi);
  }
}
