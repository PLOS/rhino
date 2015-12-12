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
import org.ambraproject.models.Article;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.service.AnnotationCrudService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.AnnotationOutputView;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class AnnotationCrudServiceImpl extends AmbraService implements AnnotationCrudService {

  @Autowired
  private RuntimeConfiguration runtimeConfiguration;

  /**
   * Fetch all annotations that belong to an article.
   *
   * @param article the article
   * @return the collection of annotations
   */
  private Collection<Annotation> fetchAllAnnotations(Article article) {
    return (List<Annotation>) hibernateTemplate.find("FROM Annotation WHERE articleID = ?", article.getID());
  }

  @Override
  public Transceiver readComments(ArticleIdentity articleIdentity) throws IOException {
    return new Transceiver() {
      @Override
      protected Collection<AnnotationOutputView> getData() throws IOException {
        Article article = (Article) DataAccessUtils.uniqueResult(
            hibernateTemplate.findByCriteria(DetachedCriteria.forClass(Article.class)
                    .setFetchMode("journals", FetchMode.JOIN)
                    .add(Restrictions.eq("doi", articleIdentity.getKey()))
            ));
        Collection<Annotation> comments = fetchAllAnnotations(article);
        AnnotationOutputView.Factory factory = new AnnotationOutputView.Factory(runtimeConfiguration, article, comments);
        return comments.stream()
            .filter(comment -> comment.getParentID() == null)
            .sorted(AnnotationOutputView.BY_DATE)
            .map(factory::buildView)
            .collect(Collectors.toList());
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  @Override
  public Transceiver readComment(DoiBasedIdentity commentId) throws IOException {
    return new Transceiver() {
      @Override
      protected AnnotationOutputView getData() throws IOException {
        Annotation annotation = (Annotation) DataAccessUtils.uniqueResult(
            hibernateTemplate.findByCriteria(DetachedCriteria.forClass(Annotation.class)
                    .add(Restrictions.eq("annotationUri", commentId.getKey()))
            ));
        if (annotation == null) {
          throw reportNotFound(commentId);
        }

        // TODO: Make this more efficient. Three queries is too many.
        Article article = (Article) DataAccessUtils.uniqueResult(
            hibernateTemplate.findByCriteria(DetachedCriteria.forClass(Article.class)
                    .setFetchMode("journals", FetchMode.JOIN)
                    .add(Restrictions.eq("ID", annotation.getArticleID()))
            ));

        return new AnnotationOutputView.Factory(runtimeConfiguration, article, fetchAllAnnotations(article)).buildView(annotation);
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

}
