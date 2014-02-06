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

import org.ambraproject.models.Annotation;
import org.ambraproject.models.AnnotationType;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.service.AnnotationCrudService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.views.AnnotationView;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.support.DataAccessUtils;

import java.io.IOException;
import java.util.ArrayList;
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
  public void readCorrections(ResponseReceiver receiver, ArticleIdentity id, MetadataFormat format)
      throws IOException {
    throw new RuntimeException("Corrections are temporarily unsupported"); // TODO: AMEC-2275
  }

  /**
   * {@inheritDoc}
   */
  public void readComments(ResponseReceiver receiver, ArticleIdentity id, MetadataFormat format)
      throws IOException {
    readAnnotations(receiver, id, format);
  }

  /**
   * Forwards annotations matching the given types to the receiver.
   *
   * @param receiver wraps the response object
   * @param id       identifies the article
   * @param format   must currently be MetadataFormat.JSON
   * @throws IOException
   */
  private void readAnnotations(ResponseReceiver receiver, ArticleIdentity id, MetadataFormat format)
      throws IOException {
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
    serializeMetadata(format, receiver, results);
  }

  /**
   * Fetch all annotations that belong to an article.
   *
   * @param article the article
   * @return the collection of annotations
   */
  private List<Annotation> fetchAllAnnotations(Article article) {
    return hibernateTemplate.find("FROM Annotation WHERE articleID = ?", article.getID());
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
  public void readComment(ResponseReceiver receiver, DoiBasedIdentity commentId, MetadataFormat format)
      throws IOException {
    readAnnotation(receiver, commentId, format);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void readCorrection(ResponseReceiver receiver, DoiBasedIdentity commentId, MetadataFormat format)
      throws IOException {
    throw new RuntimeException("Corrections are temporarily unsupported"); // TODO: AMEC-2275
  }

  private void readAnnotation(ResponseReceiver receiver, DoiBasedIdentity annotationId, MetadataFormat format)
      throws IOException {
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
            .add(Restrictions.eq("ID", annotation.getArticleID()))
        ));
    Map<Long, List<Annotation>> replies = findAnnotationReplies(annotation.getID(), fetchAllAnnotations(article),
        new LinkedHashMap<Long, List<Annotation>>());
    AnnotationView view = new AnnotationView(annotation, article.getDoi(), article.getTitle(), replies);
    serializeMetadata(format, receiver, view);
  }
}
