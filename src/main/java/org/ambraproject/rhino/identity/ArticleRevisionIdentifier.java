package org.ambraproject.rhino.identity;

import java.util.Objects;

/**
 * An identifier for one ingested version of an article.
 * <p>
 * Instances of this class can indicate only article versions that are persisted with a non-null revision number, i.e.,
 * the most recently ingested version with that number.
 */
public final class ArticleRevisionIdentifier {

  private final ArticleIdentifier articleIdentifier;
  private final int revision;

  private ArticleRevisionIdentifier(ArticleIdentifier articleIdentifier, int revision) {
    this.articleIdentifier = Objects.requireNonNull(articleIdentifier);
    this.revision = revision;
  }

  public static ArticleRevisionIdentifier create(ArticleIdentifier articleIdentifier, int revision) {
    return new ArticleRevisionIdentifier(articleIdentifier, revision);
  }

  public static ArticleRevisionIdentifier create(Doi articleIdentifier, int revision) {
    return create(ArticleIdentifier.create(articleIdentifier), revision);
  }

  public static ArticleRevisionIdentifier create(String articleIdentifier, int revision) {
    return create(ArticleIdentifier.create(articleIdentifier), revision);
  }

  public ArticleIdentifier getArticleIdentifier() {
    return articleIdentifier;
  }

  public int getRevision() {
    return revision;
  }

  // Convenience method for unpacking into persistent form
  public String getDoiName() {
    return articleIdentifier.getDoiName();
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

    ArticleRevisionIdentifier that = (ArticleRevisionIdentifier) o;

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
