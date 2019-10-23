/*
 * Copyright (c) 2019 Public Library of Science
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;

import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleRelationship;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.view.journal.JournalOutputView;
import org.springframework.beans.factory.annotation.Autowired;

public class RelationshipViewFactory {
  @Autowired
  private ArticleCrudService articleCrudService;

  public static Map<String, String> invertedTypes = ImmutableMap.<String, String>builder()
    .put("commentary", "commentary-article")
    .put("commentary-article", "commentary")
    .put("companion", "companion")
    .put("corrected-article", "correction-forward")
    .put("correction-forward", "corrected-article")
    .put("retracted-article", "retraction-forward")
    .put("retraction-forward", "retracted-article")
    .put("object-of-concern", "concern-forward")
    .put("concern-forward", "object-of-concern")
    .put("updated-article", "update-forward")
    .put("update-forward", "updated-article")
    .build();

  /* 
   * Our corpus contains some dirty data: make it canonical.
   */
  private static String getCanonicalType(String type) {
    if (type.equals("corrrection-forward")) {
      /* lol, it's too much trouble to repub this */
      return "correction-forward";
    } else if (type.equals("article-commentary")) {
      return "commentary-article";
    } else {
      return type;
    }
  }

  private RelationshipView of(Article article, String type) {
    Optional<ArticleRevision> revision = articleCrudService.getLatestRevision(article);
    Optional<ArticleIngestion> ingestion = revision.map(ArticleRevision::getIngestion);
    String title = ingestion.map(ArticleIngestion::getTitle).orElse(null);
    Integer revisionNumber = revision.map(ArticleRevision::getRevisionNumber).orElse(null);
    LocalDate publicationDate = ingestion.map(ing -> ing.getPublicationDate().toLocalDate()).orElse(null);
    JournalOutputView journal = ingestion.map(ing -> JournalOutputView.getView(ing.getJournal())).orElse(null);
    return RelationshipView.builder().setDoi(article.getDoi())
      .setTitle(title)
      .setRevisionNumber(revisionNumber)
      .setPublicationDate(publicationDate)
      .setJournal(journal)
      .setType(getCanonicalType(type))
      .build();
  }

  public RelationshipView of(ArticleRelationship relation) {
    return of(relation.getSourceArticle(), relation.getType());
  }

  public RelationshipView invert(ArticleRelationship relation) {
    String invertedRelation = invertedTypes.getOrDefault(relation.getType(), relation.getType() +"-inverted");
    return of(relation.getTargetArticle(), invertedRelation);
  }

  public List<RelationshipView> getRelationshipViews(ArticleIdentifier articleId) {
    Stream<RelationshipView> from = articleCrudService.getRelationshipsTo(articleId).stream().map(this::of);
    Stream<RelationshipView> to = articleCrudService.getRelationshipsFrom(articleId).stream().map(this::invert);
    return Stream.concat(from, to).distinct().collect(Collectors.toList());
  }
}
