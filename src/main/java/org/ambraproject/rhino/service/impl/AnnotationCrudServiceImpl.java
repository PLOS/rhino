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
import org.ambraproject.models.Flag;
import org.ambraproject.models.FlagReasonCode;
import org.ambraproject.models.UserProfile;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.AnnotationCrudService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.comment.AnnotationNodeView;
import org.ambraproject.rhino.view.comment.AnnotationOutputView;
import org.ambraproject.rhino.view.comment.CommentCount;
import org.ambraproject.rhino.view.comment.CommentFlagInputView;
import org.ambraproject.rhino.view.comment.CommentInputView;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
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
        Annotation annotation = getComment(commentId);

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

  private Annotation getComment(DoiBasedIdentity commentId) {
    Annotation annotation = (Annotation) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(DetachedCriteria.forClass(Annotation.class)
            .add(Restrictions.eq("annotationUri", commentId.getKey()))));
    if (annotation == null) {
      throw reportNotFound(commentId);
    }
    return annotation;
  }

  private UserProfile getUserProfile(String authId) {
    UserProfile creator = (UserProfile) DataAccessUtils.uniqueResult(hibernateTemplate.find(
        "FROM UserProfile WHERE authId = ?", authId));
    if (creator == null) {
      throw new RestClientException("UserProfile not found: " + authId, HttpStatus.BAD_REQUEST);
    }
    return creator;
  }

  private static final Pattern DOI_PREFIX_PATTERN = Pattern.compile("^\\d+\\.\\d+/");

  private static String extractDoiPrefix(DoiBasedIdentity doi) {
    Matcher matcher = DOI_PREFIX_PATTERN.matcher(doi.getIdentifier());
    Preconditions.checkArgument(matcher.find());
    return matcher.group();
  }

  @Override
  public Annotation createComment(CommentInputView input) {
    final Optional<DoiBasedIdentity> parentCommentUri = Optional.ofNullable(input.getParentCommentId()).map(DoiBasedIdentity::create);
    Optional<ArticleIdentity> articleDoi = Optional.ofNullable(input.getArticleDoi()).map(ArticleIdentity::create);

    final Long articlePk;
    final Optional<Long> parentCommentPk;
    final AnnotationType annotationType;
    if (parentCommentUri.isPresent()) {
      // The comment is a reply to a parent comment.
      // The client might not have declared the parent article, so look it up from the parent comment.
      Object[] parentAnnotationData = (Object[]) DataAccessUtils.uniqueResult(hibernateTemplate.find("" +
              "SELECT ann.ID, ann.articleID, art.doi " +
              "FROM Annotation ann, Article art " +
              "WHERE ann.annotationUri = ? AND ann.articleID = art.ID",
          parentCommentUri.get().getKey()));
      if (parentAnnotationData == null) {
        throw new RestClientException("Parent comment not found: " + parentCommentUri, HttpStatus.BAD_REQUEST);
      }

      parentCommentPk = Optional.of((Long) parentAnnotationData[0]);
      articlePk = (Long) parentAnnotationData[1];
      annotationType = AnnotationType.REPLY;

      ArticleIdentity articleDoiFromDb = ArticleIdentity.create((String) parentAnnotationData[2]);
      if (!articleDoi.isPresent()) {
        articleDoi = Optional.of(articleDoiFromDb);
      } else if (!articleDoi.get().equals(articleDoiFromDb)) {
        String message = String.format("Parent comment (%s) not from declared article (%s).",
            parentCommentUri.get().getKey(), articleDoi.get().getKey());
        throw new RestClientException(message, HttpStatus.BAD_REQUEST);
      }
    } else {
      // The comment is a root-level reply to an article (no parent comment).
      if (!articleDoi.isPresent()) {
        throw new RestClientException("Must provide articleDoi or parentCommentId", HttpStatus.BAD_REQUEST);
      }

      articlePk = (Long) DataAccessUtils.uniqueResult(hibernateTemplate.find(
          "SELECT ID FROM Article WHERE doi = ?", articleDoi.get().getKey()));
      if (articlePk == null) {
        throw new RestClientException("Parent article not found: " + articleDoi.get(), HttpStatus.BAD_REQUEST);
      }
      parentCommentPk = Optional.empty();
      annotationType = AnnotationType.COMMENT;
    }

    UserProfile creator = getUserProfile(input.getCreatorAuthId());

    String doiPrefix = extractDoiPrefix(articleDoi.get()); // comment receives same DOI prefix as article
    UUID uuid = UUID.randomUUID(); // generate a new DOI out of a random UUID
    DoiBasedIdentity createdAnnotationUri = DoiBasedIdentity.create(doiPrefix + "annotation/" + uuid);

    Annotation created = new Annotation();
    created.setType(annotationType);
    created.setCreator(creator);
    created.setArticleID(articlePk);
    created.setParentID(parentCommentPk.orElse(null));
    created.setAnnotationUri(createdAnnotationUri.getKey());
    created.setTitle(Strings.nullToEmpty(input.getTitle()));
    created.setBody(Strings.nullToEmpty(input.getBody()));
    created.setHighlightedText(Strings.nullToEmpty(input.getHighlightedText()));
    created.setCompetingInterestBody(Strings.nullToEmpty(input.getCompetingInterestStatement()));

    hibernateTemplate.save(created);
    return created;
  }

  @Override
  public Flag createCommentFlag(DoiBasedIdentity commentId, CommentFlagInputView input) {
    Annotation comment = getComment(commentId);
    UserProfile flagCreator = getUserProfile(input.getCreatorAuthId());

    Flag flag = new Flag();
    flag.setFlaggedAnnotation(comment);
    flag.setCreator(flagCreator);
    flag.setComment(input.getBody());
    flag.setReason(FlagReasonCode.fromString(input.getReasonCode()));

    hibernateTemplate.save(flag);
    return flag;
  }

  @Override
  public Transceiver readRecentComments(String journalKey, OptionalInt limit) {
    return new Transceiver() {
      @Override
      protected List<AnnotationNodeView> getData() throws IOException {
        List<Object[]> results = hibernateTemplate.execute(session -> {
          Query query = session.createQuery("" +
              "SELECT ann, art.doi, art.title " +
              "FROM Annotation ann, Article art, Journal j " +
              "WHERE ann.articleID = art.ID " +
              "  AND j IN ELEMENTS(art.journals) " +
              "  AND j.journalKey = :journalKey " +
              "ORDER BY ann.created DESC");
          query.setParameter("journalKey", journalKey);
          limit.ifPresent(query::setMaxResults);
          return query.list();
        });
        AnnotationNodeView.Factory viewFactory = new AnnotationNodeView.Factory(runtimeConfiguration);
        return results.stream()
            .map((Object[] result) -> {
              Annotation annotation = (Annotation) result[0];
              String articleDoi = (String) result[1];
              String articleTitle = (String) result[2];
              return viewFactory.create(annotation, journalKey, articleDoi, articleTitle);
            })
            .collect(Collectors.toList());
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  @Override
  public CommentCount getCommentCount(Article article) {
    long root = (Long) DataAccessUtils.requiredSingleResult(hibernateTemplate.find(
        "SELECT COUNT(*) FROM Annotation WHERE articleID = ? AND parentID IS NULL", article.getID()));
    long all = (Long) DataAccessUtils.requiredSingleResult(hibernateTemplate.find(
        "SELECT COUNT(*) FROM Annotation WHERE articleID = ?", article.getID()));
    return new CommentCount(root, all);
  }

}
