/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.view.article;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleRelationship;
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
        List<ArticleRelationship> relationships,
        Function<ArticleRelationship, Article> direction) {
      Objects.requireNonNull(direction);
      return relationships.stream()
          .map((ArticleRelationship var) -> {
            String type = var.getType();
            Doi doi = Doi.create(direction.apply(var).getDoi());
            Optional<ArticleRevision> revision = articleCrudService.getLatestRevision(direction.apply(var));
            return new RelationshipView(type, doi, revision);
          })
          .collect(Collectors.toList());
    }

    public RelationshipSetView getSetView(ArticleIdentifier articleId) {
      List<ArticleRelationship> inbound = articleCrudService.getRelationshipsTo(articleId);
      List<RelationshipSetView.RelationshipView> inboundViews = getRelationshipViews(inbound, ArticleRelationship::getSourceArticle);

      List<ArticleRelationship> outbound = articleCrudService.getRelationshipsFrom(articleId);
      List<RelationshipSetView.RelationshipView> outboundViews = getRelationshipViews(outbound, ArticleRelationship::getTargetArticle);

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
        this.journal = JournalOutputView.getView(ingestion.getJournal());
      } else {
        this.revisionNumber = null;
        this.title = null;
        this.publicationDate = null;
        this.journal = null;
      }
    }
  }

}
