package org.ambraproject.rhino.model.article;

import java.util.Objects;

public class AssetMetadata {

  private final String doi;
  private final String contextElement;
  private final String title;
  private final String description;

  public AssetMetadata(String doi, String contextElement, String title, String description) {
    this.doi = Objects.requireNonNull(doi);
    this.contextElement = Objects.requireNonNull(contextElement);
    this.title = Objects.requireNonNull(title);
    this.description = Objects.requireNonNull(description);
  }

  public String getDoi() {
    return doi;
  }

  public String getContextElement() {
    return contextElement;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AssetMetadata that = (AssetMetadata) o;

    if (!doi.equals(that.doi)) return false;
    if (!contextElement.equals(that.contextElement)) return false;
    if (!title.equals(that.title)) return false;
    return description.equals(that.description);

  }

  @Override
  public int hashCode() {
    int result = doi.hashCode();
    result = 31 * result + contextElement.hashCode();
    result = 31 * result + title.hashCode();
    result = 31 * result + description.hashCode();
    return result;
  }
}