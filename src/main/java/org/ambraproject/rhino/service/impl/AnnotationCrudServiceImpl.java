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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.ambraproject.models.Annotation;
import org.ambraproject.models.AnnotationType;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.service.AnnotationCrudService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.views.AnnotationView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@inheritDoc}
 */
public class AnnotationCrudServiceImpl extends AmbraService implements AnnotationCrudService {

  /**
   * AnnotationTypes that are considered to be corrections.
   */
  private static final ImmutableSet<AnnotationType> CORRECTION_TYPES = Sets.immutableEnumSet(
      AnnotationType.FORMAL_CORRECTION, AnnotationType.MINOR_CORRECTION, AnnotationType.RETRACTION);

  /**
   * {@inheritDoc}
   */
  public void readCorrections(ResponseReceiver receiver, ArticleIdentity id, MetadataFormat format)
      throws IOException {
    readAnnotations(receiver, id, format, CORRECTION_TYPES);
  }

  /**
   * {@inheritDoc}
   */
  public void readComments(ResponseReceiver receiver, ArticleIdentity id, MetadataFormat format)
      throws IOException {
    readAnnotations(receiver, id, format, Sets.immutableEnumSet(AnnotationType.COMMENT));
  }

  /**
   * Forwards annotations matching the given types to the receiver.
   *
   * @param receiver wraps the response object
   * @param id identifies the article
   * @param format must currently be MetadataFormat.JSON
   * @param annotationTypes set of annotation types to select
   * @throws IOException
   */
  private void readAnnotations(ResponseReceiver receiver, ArticleIdentity id, MetadataFormat format,
      Set<AnnotationType> annotationTypes) throws IOException {
    if (format != MetadataFormat.JSON) {
      throw new IllegalArgumentException("Only JSON is supported");
    }
    Article article = findSingleEntity("FROM Article WHERE doi = ?", id.getKey());

    // We have to get all annotations for an article, not just corrections, since we want to
    // include any replies to the corrections.
    List<Annotation> annotations = hibernateTemplate.find("FROM Annotation WHERE articleID = ?", article.getID());

    // AnnotationView is an ambra component that is convenient here since it encapsulates everything
    // we want to return about a given annotation.  It also handles nested replies.
    List<AnnotationView> results = new ArrayList<AnnotationView>(annotations.size());
    for (Annotation annotation : annotations) {
      if (annotationTypes.contains(annotation.getType())) {
        Map<Long, List<Annotation>> replies = findAnnotationReplies(annotation.getID(), annotations,
            new HashMap<Long, List<Annotation>>());
        results.add(new AnnotationView(annotation, article.getDoi(), article.getTitle(), replies));
      }
    }
    writeJson(receiver, results);
  }

  /**
   * Recursively loads all child replies for a given annotation.  That is, loads the full reply
   * tree given an annotation that is the root.
   *
   * @param annotationId specifies the root annotation
   * @param results the partial results during the recursion.  The initial caller should pass
   *     in an empty map.
   * @return a map from annotationID to list of child replies
   */
  private Map<Long, List<Annotation>> findAnnotationReplies(Long annotationId, List<Annotation> allArticleAnnotations,
      Map<Long, List<Annotation>> results) {
    List<Annotation> children = new ArrayList<Annotation>();
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
}
