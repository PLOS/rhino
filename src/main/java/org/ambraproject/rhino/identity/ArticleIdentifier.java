package org.ambraproject.rhino.identity;

import java.util.Objects;

/**
 * An identifier for an entire article, spanning all of its versions and their component items.
 */
public final class ArticleIdentifier {

  private final Doi doi;

  private ArticleIdentifier(Doi doi) {
    this.doi = Objects.requireNonNull(doi);
  }

  public static ArticleIdentifier create(Doi doi) {
    return new ArticleIdentifier(doi);
  }

  public static ArticleIdentifier create(String doi) {
    return create(Doi.create(doi));
  }

  public Doi getDoi() {
    return doi;
  }

  // Convenience method for unpacking into persistent form
  public String getDoiName() {
    return doi.getName();
  }

  @Override
  public String toString() {
    return "ArticleIdentifier{" +
        "doi=" + doi +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    return this == o || o != null && getClass() == o.getClass() && doi.equals(((ArticleIdentifier) o).doi);
  }

  @Override
  public int hashCode() {
    return doi.hashCode();
  }
}
