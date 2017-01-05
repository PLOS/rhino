package org.ambraproject.rhino.model.ingest;

import org.ambraproject.rhino.model.article.ArticleCustomMetadata;
import org.ambraproject.rhino.model.article.ArticleMetadata;

import java.util.Objects;

public class IngestPackage {
  private final ArticlePackage articlePackage;
  private final ArticleMetadata articleMetadata;
  private final ArticleCustomMetadata articleCustomMetadata;

  public IngestPackage(ArticlePackage articlePackage, ArticleMetadata articleMetadata,
                       ArticleCustomMetadata customMetadata) {
    this.articlePackage = Objects.requireNonNull(articlePackage);
    this.articleMetadata = Objects.requireNonNull(articleMetadata);
    this.articleCustomMetadata = Objects.requireNonNull(customMetadata);
  }

  public ArticlePackage getArticlePackage() {
    return articlePackage;
  }

  public ArticleMetadata getArticleMetadata() {
    return articleMetadata;
  }

  public ArticleCustomMetadata getArticleCustomMetadata() {
    return articleCustomMetadata;
  }
}
