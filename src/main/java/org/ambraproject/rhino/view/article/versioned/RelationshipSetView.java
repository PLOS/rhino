package org.ambraproject.rhino.view.article.versioned;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.VersionedArticleRelationship;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.view.journal.JournalOutputView;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RelationshipSetView {

  public static class Factory {
    @Autowired
    private ArticleCrudService articleCrudService;

    private List<RelationshipSetView.RelationshipView> getRelationshipViews(
        List<VersionedArticleRelationship> relationships,
        Function<VersionedArticleRelationship, ArticleTable> direction) {
      Objects.requireNonNull(direction);
      return relationships.stream()
          .map((VersionedArticleRelationship var) -> {
            String type = var.getType();
            Doi doi = Doi.create(direction.apply(var).getDoi());
            Optional<ArticleRevision> revision = articleCrudService.getLatestRevision(direction.apply(var));
            return new RelationshipView(type, doi, revision);
          })
          .collect(Collectors.toList());
    }

    public RelationshipSetView getSetView(ArticleIdentifier articleId) {
      List<VersionedArticleRelationship> inbound = articleCrudService.getRelationshipsTo(articleId);
      List<RelationshipSetView.RelationshipView> inboundViews = getRelationshipViews(inbound, VersionedArticleRelationship::getSourceArticle);

      List<VersionedArticleRelationship> outbound = articleCrudService.getRelationshipsFrom(articleId);
      List<RelationshipSetView.RelationshipView> outboundViews = getRelationshipViews(outbound, VersionedArticleRelationship::getTargetArticle);

      return new RelationshipSetView(inboundViews, outboundViews);
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

    // These may be null if the related article is unpublished
    private final Integer revisionNumber;
    private final String title;
    private final LocalDate publicationDate;
    private final JournalOutputView journal;

    private RelationshipView(String type, Doi doi, Optional<ArticleRevision> otherArticle) {
      this.type = Objects.requireNonNull(type);
      this.doi = doi.getName();

      if (otherArticle.isPresent()) {
        ArticleRevision revision = otherArticle.get();
        ArticleIngestion ingestion = revision.getIngestion();
        Preconditions.checkArgument(doi.equals(Doi.create(ingestion.getArticle().getDoi())));
        this.revisionNumber = revision.getRevisionNumber();
        this.title = ingestion.getTitle();
        this.publicationDate = ingestion.getPublicationDate().toLocalDate();
        this.journal = JournalOutputView.getShallowView(ingestion.getJournal());
      } else {
        this.revisionNumber = null;
        this.title = null;
        this.publicationDate = null;
        this.journal = null;
      }
    }
  }

}
