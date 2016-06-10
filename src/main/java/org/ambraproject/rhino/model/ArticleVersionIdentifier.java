package org.ambraproject.rhino.model;

public class ArticleVersionIdentifier {

  private String doi;
  private int version;

  public ArticleVersionIdentifier(String doi, int version) {
    this.doi = doi;
    this.version = version;
  }

  public String getDoi() {
    return doi;
  }

  public void setDoi(String doi) {
    this.doi = doi;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  @Override
  public String toString() {
    return "ArticleVersionIdentifier{" +
        "doi='" + doi + '\'' +
        ", version=" + version +
        '}';
  }
}
