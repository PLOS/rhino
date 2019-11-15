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
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleRelationship;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration
public class RelationshipViewFactoryTest extends AbstractJUnit4SpringContextTests {
  @Configuration
  static class ContextConfiguration {
    @Bean
    public ArticleCrudService articleCrudService() {
      return mock(ArticleCrudService.class);
    }

    @Bean
    public RelationshipViewFactory relationshipViewFactory() {
      return new RelationshipViewFactory();
    }
  }
  
  @Autowired
  private ArticleCrudService articleCrudService;

  @Autowired
  private RelationshipViewFactory relationshipViewFactory;

  private ArticleRelationship rel;
  @Test
  public void testRoundTripTypeInversion() {
    for (String type: RelationshipViewFactory.invertedTypes.keySet()) {
      String inverted = RelationshipViewFactory.invertedTypes.get(type);
      String doubleInverted = RelationshipViewFactory.invertedTypes.get(inverted);
      assertEquals(type, doubleInverted);
    }
  }

  @Before
  public void setupArticleRelations() {
    Date date = new Date(1571349735000L);
    rel = mock(ArticleRelationship.class);
    when(rel.getType()).thenReturn("corrected-article");
    Article source = mock(Article.class);
    Article target = mock(Article.class);
    ArticleIngestion sourceIngestion = mock(ArticleIngestion.class);
    ArticleIngestion targetIngestion = mock(ArticleIngestion.class);
    ArticleRevision sourceRevision = mock(ArticleRevision.class);
    ArticleRevision targetRevision = mock(ArticleRevision.class);
    Journal journal = mock(Journal.class);
    when(journal.getTitle()).thenReturn("PLOS Zero");
    when(journal.getJournalKey()).thenReturn("PLOSZERO");
    when(journal.geteIssn()).thenReturn("0000-0000");
    when(articleCrudService.getLatestRevision(source)).thenReturn(Optional.of(sourceRevision));
    when(articleCrudService.getLatestRevision(target)).thenReturn(Optional.of(targetRevision));
    when(sourceRevision.getIngestion()).thenReturn(sourceIngestion);
    when(targetRevision.getIngestion()).thenReturn(targetIngestion);
    when(sourceRevision.getRevisionNumber()).thenReturn(1);
    when(targetRevision.getRevisionNumber()).thenReturn(1);
    when(sourceIngestion.getJournal()).thenReturn(journal);
    when(targetIngestion.getJournal()).thenReturn(journal);
    when(sourceIngestion.getPublicationDate()).thenReturn(date);
    when(targetIngestion.getPublicationDate()).thenReturn(date);
    when(targetIngestion.getTitle()).thenReturn("Target");
    when(sourceIngestion.getTitle()).thenReturn("Source");
    when(source.getDoi()).thenReturn("10.9999/journal.pxxx.source");
    when(rel.getSourceArticle()).thenReturn(source);
    when(target.getDoi()).thenReturn("10.9999/journal.pxxx.target");
    when(rel.getSourceArticle()).thenReturn(source);
    when(rel.getTargetArticle()).thenReturn(target);
  }

  @Test
  public void testGetRelationshipViews() {
    when(articleCrudService.getRelationshipsFrom(any(ArticleIdentifier.class))).thenReturn(ImmutableList.of(rel));
    when(articleCrudService.getRelationshipsTo(any(ArticleIdentifier.class))).thenReturn(ImmutableList.of());
    List<RelationshipView> views = relationshipViewFactory.getRelationshipViews(ArticleIdentifier.create("10.9999/journal.xxx.1"));
    assertEquals(1, views.size());
    assertEquals("corrected-article", views.get(0).getType());
    assertEquals("Target", views.get(0).getTitle());
  }

  @Test
  public void testGetRelationshipViewsOtherDirection() {
    when(articleCrudService.getRelationshipsFrom(any(ArticleIdentifier.class))).thenReturn(ImmutableList.of());
    when(articleCrudService.getRelationshipsTo(any(ArticleIdentifier.class))).thenReturn(ImmutableList.of(rel));
    List<RelationshipView> views = relationshipViewFactory.getRelationshipViews(ArticleIdentifier.create("10.9999/journal.xxx.1"));
    assertEquals(1, views.size());
    assertEquals("correction-forward", views.get(0).getType());
    assertEquals("Source", views.get(0).getTitle());
  }

  @Test
  public void testGetRelationshipInvertUnknownType() {
    when(rel.getType()).thenReturn("new-type");
    RelationshipView view = relationshipViewFactory.invert(rel);
    assertEquals("new-type-inverted", view.getType());
  }

  @Test
  public void testCanonicalizeType() {
    when(rel.getType()).thenReturn("corrrection-forward"); // arrrrrrrrr
    RelationshipView view = relationshipViewFactory.of(rel);
    assertEquals("correction-forward", view.getType());
  }
}
