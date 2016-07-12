package org.ambraproject.rhino.view.article.versioned;

import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.VersionedArticleRelationship;
import org.ambraproject.rhino.model.article.ArticleMetadata;
import org.ambraproject.rhino.model.article.RelatedArticleLink;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.impl.VersionedIngestionService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ArticleIngestionViewFactory {

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private VersionedIngestionService versionedIngestionService;

  private RelationshipSetView getRelationships(ArticleIdentifier articleId, ArticleMetadata metadata) {
    List<VersionedArticleRelationship> inbound = articleCrudService.getArticleRelationshipsTo(articleId);
    List<VersionedArticleRelationship> outbound = articleCrudService.getArticleRelationshipsFrom(articleId);
    List<RelatedArticleLink> declared = metadata.getRelatedArticles();
    return new RelationshipSetView(outbound, inbound, declared);
  }

  public ArticleIngestionView getView(ArticleIngestionIdentifier ingestionId) {
    ArticleIngestion ingestion = articleCrudService.getArticleIngestion(ingestionId);
    ArticleMetadata metadata = versionedIngestionService.getArticleMetadata(ingestionId);

    ArticleIdentifier articleId = ArticleIdentifier.create(ingestion.getArticle().getDoi());
    RelationshipSetView relationships = getRelationships(articleId, metadata);

    return new ArticleIngestionView(ingestion, metadata, relationships);
  }

}
