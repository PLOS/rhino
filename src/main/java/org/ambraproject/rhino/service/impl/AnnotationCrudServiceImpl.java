/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2013 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.service.impl;

import org.ambraproject.models.AmbraEntity;
import org.ambraproject.models.Annotation;
import org.ambraproject.models.AnnotationType;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.service.AnnotationCrudService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.AnnotationOutputView;
import org.ambraproject.views.AnnotationView;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.support.DataAccessUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@inheritDoc}
 */
public class AnnotationCrudServiceImpl extends AmbraService implements AnnotationCrudService {

  /**
   * {@inheritDoc}
   */
  public Transceiver readComments(ArticleIdentity id)
      throws IOException {
    return readAnnotations(id);
  }

  private static Date getLatestLastModified(Iterable<? extends AmbraEntity> values) {
    Date lastOfAll = null;
    for (AmbraEntity entity : values) {
      Date lastModified = entity.getLastModified();
      if (lastOfAll == null || (lastModified != null && lastModified.after(lastOfAll))) {
        lastOfAll = lastModified;
      }
    }
    return lastOfAll;
  }

  /**
   * Forwards annotations matching the given types to the receiver.
   *
   * @param id identifies the article
   * @throws IOException
   */
  private Transceiver readAnnotations(final ArticleIdentity id)
      throws IOException {
    return new Transceiver() {
      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        // Requires searching of nested replies. Unsupported for now.
        return null;
      }

      @Override
      protected Object getData() throws IOException {
        Article article = findSingleEntity("FROM Article WHERE doi = ?", id.getKey());
        List<Annotation> annotations = fetchAllAnnotations(article);

        // AnnotationView is an ambra component that is convenient here since it encapsulates everything
        // we want to return about a given annotation.  It also handles nested replies.
        List<AnnotationView> results = new ArrayList<>(annotations.size());
        for (Annotation annotation : annotations) {
          if (AnnotationType.COMMENT.equals(annotation.getType())) {
            Map<Long, List<Annotation>> replies = findAnnotationReplies(annotation.getID(), annotations,
                new HashMap<Long, List<Annotation>>());
            results.add(new AnnotationView(annotation, article.getDoi(), article.getTitle(), replies));
          }
        }
        return results;
      }
    };
  }

  /**
   * Fetch all annotations that belong to an article.
   *
   * @param article the article
   * @return the collection of annotations
   */
  private List<Annotation> fetchAllAnnotations(Article article) {
    return (List<Annotation>) hibernateTemplate.find("FROM Annotation WHERE articleID = ?", article.getID());
  }

  /**
   * Recursively loads all child replies for a given annotation.  That is, loads the full reply tree given an annotation
   * that is the root.
   *
   * @param annotationId specifies the root annotation
   * @param results      the partial results during the recursion.  The initial caller should pass in an empty map.
   * @return a map from annotationID to list of child replies
   */
  private Map<Long, List<Annotation>> findAnnotationReplies(Long annotationId, List<Annotation> allArticleAnnotations,
                                                            Map<Long, List<Annotation>> results) {
    List<Annotation> children = new ArrayList<>();
    for (Annotation annotation : allArticleAnnotations) {
      if (annotationId.equals(annotation.getParentID())) {
        children.add(annotation);
      }
    }

    if (children != null && !children.isEmpty()) {
      results.put(annotationId, children);
      for (Annotation child : children) {
        results = findAnnotationReplies(child.getID(), allArticleAnnotations, results);
      }
    }
    return results;
  }

  @Override
  public Transceiver readComment(DoiBasedIdentity commentId)
      throws IOException {
    return readAnnotation(commentId);
  }

  private Transceiver readAnnotation(final DoiBasedIdentity annotationId)
      throws IOException {
    return new Transceiver() {
      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        // Requires searching of nested replies. Unsupported for now.
        return null;
      }

      @Override
      protected Object getData() throws IOException {
        Annotation annotation = (Annotation) DataAccessUtils.uniqueResult((List<?>)
            hibernateTemplate.findByCriteria(DetachedCriteria.forClass(Annotation.class)
                    .add(Restrictions.eq("annotationUri", annotationId.getKey()))
            ));
        if (annotation == null) {
          throw reportNotFound(annotationId);
        }

        // TODO: Make this more efficient. Three queries is too many.
        Article article = (Article) DataAccessUtils.uniqueResult((List<?>)
            hibernateTemplate.findByCriteria(DetachedCriteria.forClass(Article.class)
                    .setFetchMode("journals", FetchMode.JOIN)
                    .add(Restrictions.eq("ID", annotation.getArticleID()))
            ));
        Map<Long, List<Annotation>> replies = findAnnotationReplies(annotation.getID(), fetchAllAnnotations(article),
            new LinkedHashMap<Long, List<Annotation>>());

        return new AnnotationOutputView(annotation, article, replies);
      }
    };
  }
}
