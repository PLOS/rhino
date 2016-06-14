package org.ambraproject.rhino.identity;

import java.util.Objects;

public final class ArticleVersionIdentifier {

  private final ArticleIdentifier articleIdentifier;
  private final int revision;

  private ArticleVersionIdentifier(ArticleIdentifier articleIdentifier, int revision) {
    this.articleIdentifier = Objects.requireNonNull(articleIdentifier);
    this.revision = revision;
  }

  public static ArticleVersionIdentifier create(ArticleIdentifier articleIdentifier, int revision) {
    return new ArticleVersionIdentifier(articleIdentifier, revision);
  }

  public static ArticleVersionIdentifier create(String articleIdentifier, int revision) {
    return create(ArticleIdentifier.create(articleIdentifier), revision);
  }

  public ArticleIdentifier getArticleIdentifier() {
    return articleIdentifier;
  }

  public int getRevision() {
    return revision;
  }

  @Override
  public String toString() {
    return "ArticleVersionIdentifier{" +
        "articleIdentifier=" + articleIdentifier +
        ", revision=" + revision +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleVersionIdentifier that = (ArticleVersionIdentifier) o;

    if (revision != that.revision) return false;
    return articleIdentifier.equals(that.articleIdentifier);
  }

  @Override
  public int hashCode() {
    int result = articleIdentifier.hashCode();
    result = 31 * result + revision;
    return result;
  }

}
