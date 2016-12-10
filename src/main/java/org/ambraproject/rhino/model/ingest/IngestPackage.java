package org.ambraproject.rhino.model.ingest;

import org.ambraproject.rhino.model.article.ArticleCustomMetadata;
import org.ambraproject.rhino.model.article.ArticleMetadata;

public class IngestPackage {
  ArticlePackage articlePackage;
  ArticleMetadata articleMetadata;
  ArticleCustomMetadata articleCustomMetadata;

  public IngestPackage(ArticlePackage articlePackage, ArticleMetadata articleMetadata,
                       ArticleCustomMetadata customMetadata) {
    this.articlePackage = articlePackage;
    this.articleMetadata = articleMetadata;
    this.articleCustomMetadata = customMetadata;
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
