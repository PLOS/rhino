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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.ambraproject.models.Annotation;
import org.ambraproject.models.AnnotationType;
import org.ambraproject.models.Article;
import org.ambraproject.models.UserProfile;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.AnnotationCrudService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.AnnotationOutputView;
import org.ambraproject.rhino.view.comment.CommentInputView;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

  private static final Pattern DOI_PREFIX_PATTERN = Pattern.compile("^\\d+\\.\\d+/");

  private static String extractDoiPrefix(DoiBasedIdentity doi) {
    Matcher matcher = DOI_PREFIX_PATTERN.matcher(doi.getIdentifier());
    Preconditions.checkArgument(matcher.find());
    return matcher.group();
  }

  @Override
  public Annotation createComment(CommentInputView input) {
    ArticleIdentity articleDoi = ArticleIdentity.create(input.getArticleDoi());
    Long articlePk = (Long) DataAccessUtils.uniqueResult(hibernateTemplate.find(
        "SELECT ID FROM Article WHERE doi = ?", articleDoi.getKey()));
    if (articlePk == null) {
      throw new RestClientException("Parent article not found: " + articleDoi, HttpStatus.BAD_REQUEST);
    }

    final AnnotationType annotationType;
    final Long parentCommentPk;
    String parentCommentUri = input.getParentCommentId();
    if (Strings.isNullOrEmpty(parentCommentUri)) {
      annotationType = AnnotationType.COMMENT;
      parentCommentPk = null;
    } else {
      Object[] parentAnnotationData = (Object[]) DataAccessUtils.uniqueResult(hibernateTemplate.find(
          "SELECT ID, articleID FROM Annotation WHERE annotationUri = ?", DoiBasedIdentity.create(parentCommentUri).getKey()));
      if (parentAnnotationData == null) {
        throw new RestClientException("Parent comment not found: " + parentCommentUri, HttpStatus.BAD_REQUEST);
      }
      parentCommentPk = (Long) parentAnnotationData[0];
      annotationType = AnnotationType.REPLY;

      Long parentCommentArticlePk = (Long) parentAnnotationData[1];
      if (!articlePk.equals(parentCommentArticlePk)) {
        throw new RestClientException("Parent comment not from same article.", HttpStatus.BAD_REQUEST);
      }
    }

    UserProfile creator = (UserProfile) DataAccessUtils.uniqueResult(hibernateTemplate.find(
        "FROM UserProfile WHERE authId = ?", input.getCreatorAuthId()));
    if (creator == null) {
      throw new RestClientException("UserProfile not found: " + input.getCreatorAuthId(), HttpStatus.BAD_REQUEST);
    }

    String doiPrefix = extractDoiPrefix(articleDoi); // comment receives same DOI prefix as article
    UUID uuid = UUID.randomUUID(); // generate a new DOI out of a random UUID
    DoiBasedIdentity createdAnnotationUri = DoiBasedIdentity.create(doiPrefix + "annotation/" + uuid);

    Annotation created = new Annotation();
    created.setType(annotationType);
    created.setCreator(creator);
    created.setArticleID(articlePk);
    created.setParentID(parentCommentPk);
    created.setAnnotationUri(createdAnnotationUri.getKey());
    created.setTitle(Strings.nullToEmpty(input.getTitle()));
    created.setBody(Strings.nullToEmpty(input.getBody()));
    created.setHighlightedText(Strings.nullToEmpty(input.getHighlightedText()));
    created.setCompetingInterestBody(Strings.nullToEmpty(input.getCompetingInterestStatement()));

    hibernateTemplate.save(created);
    return created;
  }

}
