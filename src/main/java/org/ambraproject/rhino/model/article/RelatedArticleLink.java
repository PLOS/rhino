package org.ambraproject.rhino.model.article;

import org.ambraproject.rhino.identity.Doi;

import java.util.Objects;

public class RelatedArticleLink {

  private final String type;
  private final Doi doi;

  public RelatedArticleLink(String type, String href) {
    this.type = Objects.requireNonNull(type);
    this.doi = Doi.create(Objects.requireNonNull(href));
  }

  public String getType() {
    return type;
  }

  public Doi getDoi() {
    return doi;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RelatedArticleLink that = (RelatedArticleLink) o;

    if (!type.equals(that.type)) return false;
    return doi.equals(that.doi);

  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + doi.hashCode();
    return result;
  }
}
