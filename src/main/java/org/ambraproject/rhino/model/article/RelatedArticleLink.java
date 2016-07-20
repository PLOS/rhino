package org.ambraproject.rhino.model.article;

import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.Doi;

import java.util.Objects;

public class RelatedArticleLink {

  private final String type;
  private final ArticleIdentifier articleId;

  public RelatedArticleLink(String type, ArticleIdentifier articleId) {
    this.type = Objects.requireNonNull(type);
    this.articleId = Objects.requireNonNull(articleId);
  }

  public String getType() {
    return type;
  }

  public ArticleIdentifier getArticleId() {
    return articleId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RelatedArticleLink that = (RelatedArticleLink) o;

    if (!type.equals(that.type)) return false;
    return articleId.equals(that.articleId);

  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + articleId.hashCode();
    return result;
  }
}
