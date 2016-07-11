package org.ambraproject.rhino.model.article;

import java.util.Objects;

public class RelatedArticleLink {

  private final String type;
  private final String href;

  public RelatedArticleLink(String type, String href) {
    this.type = Objects.requireNonNull(type);
    this.href = Objects.requireNonNull(href);
  }

  public String getType() {
    return type;
  }

  public String getHref() {
    return href;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RelatedArticleLink that = (RelatedArticleLink) o;

    if (!type.equals(that.type)) return false;
    return href.equals(that.href);
  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + href.hashCode();
    return result;
  }

}
