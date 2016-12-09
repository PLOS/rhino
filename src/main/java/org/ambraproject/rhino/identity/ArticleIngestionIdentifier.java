package org.ambraproject.rhino.identity;

import org.ambraproject.rhino.model.ArticleIngestion;

import java.util.Objects;

public final class ArticleIngestionIdentifier {

  private final ArticleIdentifier articleIdentifier;
  private final int ingestionNumber;

  private ArticleIngestionIdentifier(ArticleIdentifier articleIdentifier, int ingestionNumber) {
    this.articleIdentifier = Objects.requireNonNull(articleIdentifier);
    this.ingestionNumber = ingestionNumber;
  }

  public static ArticleIngestionIdentifier create(ArticleIdentifier articleIdentifier, int ingestionNumber) {
    return new ArticleIngestionIdentifier(articleIdentifier, ingestionNumber);
  }

  public static ArticleIngestionIdentifier create(Doi articleIdentifier, int ingestionNumber) {
    return create(ArticleIdentifier.create(articleIdentifier), ingestionNumber);
  }

  public static ArticleIngestionIdentifier create(String articleIdentifier, int ingestionNumber) {
    return create(ArticleIdentifier.create(articleIdentifier), ingestionNumber);
  }

  public static ArticleIngestionIdentifier of(ArticleIngestion ingestion) {
    return create(ArticleIdentifier.create(ingestion.getArticle().getDoi()), ingestion.getIngestionNumber());
  }

  public ArticleIdentifier getArticleIdentifier() {
    return articleIdentifier;
  }

  public int getIngestionNumber() {
    return ingestionNumber;
  }

  // Convenience method for unpacking into persistent form
  public String getDoiName() {
    return articleIdentifier.getDoiName();
  }

  /**
   * @return the identifier for the article item containing this version's manuscript
   */
  public ArticleItemIdentifier getItemFor() {
    return ArticleItemIdentifier.create(articleIdentifier.getDoi(), ingestionNumber);
  }

  @Override
  public String toString() {
    return "ArticleIngestionIdentifier{" +
        "articleIdentifier=" + articleIdentifier +
        ", ingestionNumber=" + ingestionNumber +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleIngestionIdentifier that = (ArticleIngestionIdentifier) o;

    if (ingestionNumber != that.ingestionNumber) return false;
    return articleIdentifier.equals(that.articleIdentifier);

  }

  @Override
  public int hashCode() {
    int result = articleIdentifier.hashCode();
    result = 31 * result + ingestionNumber;
    return result;
  }

}
