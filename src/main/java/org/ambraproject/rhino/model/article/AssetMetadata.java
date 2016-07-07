package org.ambraproject.rhino.model.article;

public class AssetMetadata {

  private String doi;
  private String contextElement;
  private String title;
  private String description;

  public AssetMetadata() {
  }

  public String getDoi() {
    return doi;
  }

  public void setDoi(String doi) {
    this.doi = doi;
  }

  public String getContextElement() {
    return contextElement;
  }

  public void setContextElement(String contextElement) {
    this.contextElement = contextElement;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}