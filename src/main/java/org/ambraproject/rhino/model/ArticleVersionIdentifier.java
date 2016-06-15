package org.ambraproject.rhino.model;

public class ArticleVersionIdentifier {

  private final String doi;
  private final int revision;

  public ArticleVersionIdentifier(String doi, int revision) {
    this.doi = doi;
    this.revision = revision;
  }

  public String getDoi() {
    return doi;
  }

  public int getRevision() {
    return revision;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleVersionIdentifier that = (ArticleVersionIdentifier) o;

    if (revision != that.revision) return false;
    return doi.equals(that.doi);

  }

  @Override
  public int hashCode() {
    int result = doi.hashCode();
    result = 31 * result + revision;
    return result;
  }

  @Override
  public String toString() {
    return "ArticleVersionIdentifier{" +
        "doi='" + doi + '\'' +
        ", version=" + revision +
        '}';
  }
}
