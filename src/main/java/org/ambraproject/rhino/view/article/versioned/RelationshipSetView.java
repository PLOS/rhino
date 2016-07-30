package org.ambraproject.rhino.view.article.versioned;

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.VersionedArticleRelationship;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RelationshipSetView {

  private static final Logger log = LoggerFactory.getLogger(RelationshipSetView.class);

  public static class Factory {

    @Autowired
    private ArticleCrudService articleCrudService;

    private List<RelationshipSetView.RelationshipView> getRelationshipViews(
        List<VersionedArticleRelationship> relationships,
        Function<VersionedArticleRelationship, ArticleTable> direction) {
      Objects.requireNonNull(direction);
      return relationships.stream()
          .map((VersionedArticleRelationship var) -> {
            // The view will display title and publication date from the most recent revision.
            // If the article has no revisions, suppress it from the RelationshipSetView entirely
            // (so that, e.g., unpublished amendment notices don't show up while being QC'ed).
            Optional<ArticleRevision> revision = articleCrudService.getLatestRevision(direction.apply(var));
            return revision.map((ArticleRevision rev) -> {
              String type = var.getType();
              Doi doi = Doi.create(direction.apply(var).getDoi());
              ArticleIngestion ingestion = rev.getIngestion();
              return new RelationshipView(type, doi, ingestion);
            });
          })
          .filter(Optional::isPresent).map(Optional::get)
          .collect(Collectors.toList());
    }

    public RelationshipSetView getSetView(ArticleIdentifier articleId) {
      List<VersionedArticleRelationship> inbound = articleCrudService.getRelationshipsTo(articleId);
      List<RelationshipSetView.RelationshipView> inboundViews = getRelationshipViews(inbound, VersionedArticleRelationship::getSourceArticle);

      List<VersionedArticleRelationship> outbound = articleCrudService.getRelationshipsFrom(articleId);
      List<RelationshipSetView.RelationshipView> outboundViews = getRelationshipViews(outbound, VersionedArticleRelationship::getTargetArticle);

      return new RelationshipSetView(inboundViews, outboundViews);
    }

    private ArticleIngestion getCurrentVersion(ArticleTable article) {
      return articleCrudService.getLatestRevision(article)
          .map(ArticleRevision::getIngestion)
          .orElseGet(() -> articleCrudService.getLatestIngestion(article));
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
    private final LocalDate publicationDate;

    private RelationshipView(String type, Doi doi, ArticleIngestion otherArticle) {
      this.type = Objects.requireNonNull(type);
      this.doi = doi.getName();
      this.title = otherArticle.getTitle();
      this.publicationDate = otherArticle.getPublicationDate().toLocalDate();
    }
  }

}
