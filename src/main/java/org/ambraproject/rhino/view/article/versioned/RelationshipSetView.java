package org.ambraproject.rhino.view.article.versioned;

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.VersionedArticleRelationship;
import org.ambraproject.rhino.model.article.ArticleMetadata;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.NoSuchArticleIdException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RelationshipSetView {

  private static final Logger log = LoggerFactory.getLogger(RelationshipSetView.class);

  public static class Factory {

    @Autowired
    private ArticleCrudService articleCrudService;

    public RelationshipSetView getView(ArticleMetadata metadata) {

      ArticleIdentifier articleId = ArticleIdentifier.create(metadata.getDoi());
      List<VersionedArticleRelationship> inbound = articleCrudService.getArticleRelationshipsTo(articleId);
      List<VersionedArticleRelationship> outbound = articleCrudService.getArticleRelationshipsFrom(articleId);

      List<RelationshipSetView.RelationshipView> inboundViews = inbound
          .stream().map(var -> new RelationshipSetView.RelationshipView(var.getType(),
              Doi.create(var.getSourceArticle().getDoi()),
                      getArticleTitle(var.getSourceArticle())))
          .collect(Collectors.toList());
      List<RelationshipSetView.RelationshipView> outboundViews = outbound
          .stream().map(var -> new RelationshipSetView.RelationshipView(var.getType(),
              Doi.create(var.getTargetArticle().getDoi()),
              getArticleTitle(var.getTargetArticle())))
          .collect(Collectors.toList());

      return new RelationshipSetView(inboundViews, outboundViews);

    }

    private String getArticleTitle(ArticleTable article) {

      ArticleIngestion ingestion;
      try {
        //TODO: think through what happens in QC case when no revision yet exists
        ingestion = articleCrudService.getLatestArticleRevision(article).getIngestion();
      } catch (NoSuchArticleIdException e) {
        log.error("Could not retrieve latest revision for " + article.getDoi(), e);
        throw new RuntimeException(e);
      }
      return ingestion.getArticleTitle();
    }
  }

  private final ImmutableList<RelationshipView> inbound;
  private final ImmutableList<RelationshipView> outbound;

  private RelationshipSetView(List<RelationshipView> inbound,
                              List<RelationshipView> outbound) {
    this.inbound = ImmutableList.copyOf(inbound);
    this.outbound = ImmutableList.copyOf(outbound);
  }

  public static class RelationshipView {
    private final String type;
    private final String doi;
    private final String title;

    private RelationshipView(String type, Doi doi, String title) {
      this.type = Objects.requireNonNull(type);
      this.doi = doi.getName();
      this.title = Objects.requireNonNull(title);
    }
  }

}
