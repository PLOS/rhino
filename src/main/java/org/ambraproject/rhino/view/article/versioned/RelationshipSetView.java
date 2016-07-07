package org.ambraproject.rhino.view.article.versioned;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.VersionedArticleRelationship;
import org.ambraproject.rhino.model.article.RelatedArticleLink;

import java.util.List;
import java.util.Objects;

public class RelationshipSetView {

  private final ImmutableList<RelationshipView> inbound;
  private final ImmutableList<RelationshipView> outbound;
  private final ImmutableList<RelationshipView> declared;

  RelationshipSetView(List<VersionedArticleRelationship> inbound,
                      List<VersionedArticleRelationship> outbound,
                      List<RelatedArticleLink> declared) {
    this.inbound = ImmutableList.copyOf(Lists.transform(inbound, RelationshipSetView::inboundView));
    this.outbound = ImmutableList.copyOf(Lists.transform(outbound, RelationshipSetView::outboundView));
    this.declared = ImmutableList.copyOf(Lists.transform(declared, RelationshipSetView::declaredView));
  }

  private static RelationshipView buildRelationshipView(VersionedArticleRelationship relationship, ArticleTable otherArticle) {
    String title = null; // TODO: Get title from otherArticle
    return new RelationshipView(relationship.getType(), otherArticle.getDoi(), title);
  }

  private static RelationshipView inboundView(VersionedArticleRelationship relationship) {
    return buildRelationshipView(relationship, relationship.getSourceArticle());
  }

  private static RelationshipView outboundView(VersionedArticleRelationship relationship) {
    return buildRelationshipView(relationship, relationship.getTargetArticle());
  }

  private static RelationshipView declaredView(RelatedArticleLink declaredRelationship) {
    return new RelationshipView(declaredRelationship.getType(), declaredRelationship.getHref(), null);
  }

  private static class RelationshipView {
    private final String type;
    private final String doi;
    private final String title; // nullable

    private RelationshipView(String type, String doi, String title) {
      this.type = Objects.requireNonNull(type);
      this.doi = Objects.requireNonNull(doi);
      this.title = title;
    }
  }

}
