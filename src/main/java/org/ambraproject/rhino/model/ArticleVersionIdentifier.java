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
  public String toString() {
    return "ArticleVersionIdentifier{" +
        "doi='" + doi + '\'' +
        ", version=" + revision +
        '}';
  }
}
