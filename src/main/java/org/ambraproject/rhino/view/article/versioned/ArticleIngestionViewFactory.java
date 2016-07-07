package org.ambraproject.rhino.view.article.versioned;

import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.article.ArticleMetadata;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.impl.VersionedIngestionService;
import org.springframework.beans.factory.annotation.Autowired;

public class ArticleIngestionViewFactory {

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private VersionedIngestionService versionedIngestionService;

  public ArticleIngestionView getView(ArticleIngestionIdentifier ingestionId) {
    ArticleIngestion ingestion = articleCrudService.getArticleIngestion(ingestionId);
    ArticleMetadata metadata = versionedIngestionService.getArticleMetadata(ingestionId);
    return new ArticleIngestionView(ingestion, metadata);
  }

}
