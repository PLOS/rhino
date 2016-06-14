package org.ambraproject.rhino.identity;

import java.util.Objects;

public final class ArticleItemIdentifier {

  private final Doi doi;
  private final int revision;

  private ArticleItemIdentifier(Doi doi, int revision) {
    this.doi = Objects.requireNonNull(doi);
    this.revision = revision;
  }

  public static ArticleItemIdentifier create(Doi doi, int revision) {
    return new ArticleItemIdentifier(doi, revision);
  }

  public static ArticleItemIdentifier create(String doi, int revision) {
    return create(Doi.create(doi), revision);
  }

  public Doi getDoi() {
    return doi;
  }

  public int getRevision() {
    return revision;
  }

  @Override
  public String toString() {
    return "ArticleItemIdentifier{" +
        "doi=" + doi +
        ", revision='" + revision + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleItemIdentifier that = (ArticleItemIdentifier) o;

    if (revision != that.revision) return false;
    return doi.equals(that.doi);

  }

  @Override
  public int hashCode() {
    int result = doi.hashCode();
    result = 31 * result + revision;
    return result;
  }

}
