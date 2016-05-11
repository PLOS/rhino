package org.ambraproject.rhino.content.xml;

public class AssetBuilder {

  private String doi;
  private String contextElement;
  private String title;
  private String description;

  public void setDoi(String doi) {
    this.doi = doi;
  }

  public String getDoi() {
    return doi;
  }

  public void setContextElement(String contextElement) {
    this.contextElement = contextElement;
  }

  public String getContextElement() {
    return contextElement;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
