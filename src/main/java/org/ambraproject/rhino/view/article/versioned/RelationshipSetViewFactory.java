package org.ambraproject.rhino.view.article.versioned;

import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.VersionedArticleRelationship;
import org.ambraproject.rhino.model.article.ArticleMetadata;
import org.ambraproject.rhino.model.article.RelatedArticleLink;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.NoSuchArticleIdException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RelationshipSetViewFactory {

  @Autowired
  private ArticleCrudService articleCrudService;

  private static final Logger log = LoggerFactory.getLogger(RelationshipSetViewFactory.class);

  // used to store titles for DOIs to prevent redundant fetching/parsing of manuscripts
  // TODO: create a proper cache for titles and other XML-derived data
  private Map<Doi, String> articleTitles = new HashMap<>();

    public RelationshipSetView getView(ArticleMetadata metadata) {
      ArticleIdentifier articleId = ArticleIdentifier.create(metadata.getDoi());
      List<VersionedArticleRelationship> inbound = articleCrudService.getArticleRelationshipsTo(articleId);
      List<VersionedArticleRelationship> outbound = articleCrudService.getArticleRelationshipsFrom(articleId);
      List<RelatedArticleLink> declared = metadata.getRelatedArticles();

      List<RelationshipSetView.RelationshipView> inboundViews = inbound
          .stream().map(var -> new RelationshipSetView.RelationshipView(var.getType(),
              Doi.create(var.getSourceArticle().getDoi()),
              getArticleTitle(Doi.create(var.getSourceArticle().getDoi()))))
          .collect(Collectors.toList());
      List<RelationshipSetView.RelationshipView> outboundViews = outbound
          .stream().map(var -> new RelationshipSetView.RelationshipView(var.getType(),
              Doi.create(var.getTargetArticle().getDoi()),
              getArticleTitle(Doi.create(var.getTargetArticle().getDoi()))))
          .collect(Collectors.toList());
      List<RelationshipSetView.RelationshipView> declaredViews = declared
          .stream().map(ral -> new RelationshipSetView.RelationshipView(ral.getType(),
              ral.getDoi(),
              getArticleTitle(ral.getDoi())))
          .collect(Collectors.toList());

      return new RelationshipSetView(inboundViews, outboundViews, declaredViews);

  }

  private String getArticleTitle(Doi doi) {
    String articleTitle;
    if (articleTitles.containsKey(doi)) { // null is a valid value so can't assume a null value on get() is a miss
      articleTitle = articleTitles.get(doi);
    } else {
      ArticleTable article;
      // fetch and parse XML manuscript
      try {
        article = articleCrudService.getArticle(ArticleIdentifier.create(doi));
      } catch (NoSuchArticleIdException e) {
        // DOI does not represent an article in our corpus. This is a valid condition for external references
        articleTitles.put(doi, null);
        return null;
      }
      ArticleIngestion ingestion;
      try {
        ingestion = articleCrudService.getLatestArticleRevision(article).getIngestion();
      } catch (NoSuchArticleIdException e) {
        log.error("Could not retrieve latest revision for " + article.getDoi(), e);
        throw new RuntimeException(e);
      }
      ArticleXml targetArticleXml;
      try {
        targetArticleXml = new ArticleXml(articleCrudService.getManuscriptXml(ingestion));
      } catch (IOException e) {
        log.error("Could not retrieve manuscript for " + ingestion, e);
        throw new RuntimeException(e);
      }
      articleTitle = targetArticleXml.parseTitle();
      articleTitles.put(doi, articleTitle);
    }
    return articleTitle;
  }
}
