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
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.Comment;
import org.ambraproject.rhino.model.Flag;
import org.ambraproject.rhino.model.FlagReasonCode;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.CommentCrudService;
import org.ambraproject.rhino.util.response.EntityTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.comment.CommentFlagInputView;
import org.ambraproject.rhino.view.comment.CommentFlagOutputView;
import org.ambraproject.rhino.view.comment.CommentInputView;
import org.ambraproject.rhino.view.comment.CommentNodeView;
import org.ambraproject.rhino.view.comment.CommentOutputView;
import org.hibernate.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("JpaQlInspection")
public class CommentCrudServiceImpl extends AmbraService implements CommentCrudService {

  @Autowired
  private RuntimeConfiguration runtimeConfiguration;

  @Autowired
  private ArticleCrudService articleCrudService;

  /**
   * Fetch all annotations that belong to an article.
   *
   * @param article the article
   * @return the collection of annotations
   */
  private Collection<Comment> fetchAllComments(ArticleTable article) {
    return (List<Comment>) hibernateTemplate.find("FROM comment WHERE articleId = ?", article.getArticleId());
  }

  @Override
  public Transceiver readComments(ArticleIdentifier articleId) throws IOException {
    return new Transceiver() {
      @Override
      protected Collection<CommentOutputView> getData() throws IOException {
        ArticleTable article = articleCrudService.getArticle(articleId);
        Collection<Comment> comments = fetchAllComments(article);
        CommentOutputView.Factory factory = new CommentOutputView.Factory(runtimeConfiguration, comments);
        return comments.stream()
            .filter(comment -> comment.getParent() == null)
            .sorted(CommentOutputView.BY_DATE)
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
      protected CommentOutputView getData() throws IOException {
        Comment comment = getComment(commentId);
        return new CommentOutputView.Factory(runtimeConfiguration,
            fetchAllComments(comment.getArticle())).buildView(comment);
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  private Comment getComment(DoiBasedIdentity commentId) {
    Comment comment = hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM Comment WHERE commentUri = :commentUri");
      query.setParameter("commentUri", commentId.getKey());
      return (Comment) query.uniqueResult();
    });
    if (comment == null) {
      throw reportNotFound(commentId);
    }
    return comment;
  }

  private Flag getFlag(String flagId) {
    Flag flag = hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM CommentFlag WHERE flagId = :flagId");
      query.setParameter("flagId", flagId);
      return (Flag) query.uniqueResult();
    });
    if (flag == null) {
      String message = "Comment flag not found at the provided ID: " + flagId;
      throw new RestClientException(message, HttpStatus.NOT_FOUND);
    }
    return flag;
  }

  private static final Pattern DOI_PREFIX_PATTERN = Pattern.compile("^\\d+\\.\\d+/");

  private static String extractDoiPrefix(DoiBasedIdentity doi) {
    Matcher matcher = DOI_PREFIX_PATTERN.matcher(doi.getIdentifier());
    Preconditions.checkArgument(matcher.find());
    return matcher.group();
  }

  @Override
  public Comment createComment(CommentInputView input) {
    final Optional<DoiBasedIdentity> parentCommentUri = Optional.ofNullable(input.getParentCommentId()).map(DoiBasedIdentity::create);
    Optional<ArticleIdentity> articleDoi = Optional.ofNullable(input.getArticleDoi()).map(ArticleIdentity::create);

    final ArticleTable article;
    final Comment parentComment;
    if (parentCommentUri.isPresent()) {
      parentComment = getComment(DoiBasedIdentity.create(parentCommentUri.get().getKey()));
      if (parentComment == null) {
        throw new RestClientException("Parent comment not found: " + parentCommentUri, HttpStatus.BAD_REQUEST);
      }

      article = parentComment.getArticle();

      ArticleIdentity articleDoiFromDb = ArticleIdentity.create(parentComment.getArticle().getDoi());
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

      article = articleCrudService.getArticle(ArticleIdentifier.create(articleDoi.get().getKey()));
      parentComment = null;
    }

    String doiPrefix = extractDoiPrefix(articleDoi.get()); // comment receives same DOI prefix as article
    UUID uuid = UUID.randomUUID(); // generate a new DOI out of a random UUID
    DoiBasedIdentity createdCommentUri = DoiBasedIdentity.create(doiPrefix + "annotation/" + uuid);

    Comment created = new Comment();
    created.setArticle(article);
    created.setParent(parentComment);
    created.setCommentUri(createdCommentUri.getKey());
    created.setUserProfileID(Long.valueOf(Strings.nullToEmpty(input.getCreatorUserId())));
    created.setTitle(Strings.nullToEmpty(input.getTitle()));
    created.setBody(Strings.nullToEmpty(input.getBody()));
    created.setHighlightedText(Strings.nullToEmpty(input.getHighlightedText()));
    created.setCompetingInterestBody(Strings.nullToEmpty(input.getCompetingInterestStatement()));
    created.setIsRemoved(Boolean.valueOf(Strings.nullToEmpty(input.getIsRemoved())));
    created.setLastModified(Date.from(Instant.now()));

    hibernateTemplate.save(created);
    return created;
  }

  @Override
  public Comment patchComment(DoiBasedIdentity commentId, CommentInputView input) {
    Comment comment = getComment(commentId);

    String declaredUri = input.getAnnotationUri();
    if (declaredUri != null && !DoiBasedIdentity.create(declaredUri).equals(commentId)) {
      throw new RestClientException("Mismatched annotationUri in body", HttpStatus.BAD_REQUEST);
    }

    String creatorUserId = input.getCreatorUserId();
    if (creatorUserId != null) {
      comment.setUserProfileID(Long.valueOf(creatorUserId));
    }

    String title = input.getTitle();
    if (title != null) {
      comment.setTitle(title);
    }

    String body = input.getBody();
    if (body != null) {
      comment.setBody(body);
    }

    String highlightedText = input.getHighlightedText();
    if (highlightedText != null) {
      comment.setHighlightedText(highlightedText);
    }

    String competingInterestStatement = input.getCompetingInterestStatement();
    if (competingInterestStatement != null) {
      comment.setCompetingInterestBody(competingInterestStatement);
    }

    String isRemoved = input.getIsRemoved();
    if (isRemoved != null) {
      comment.setIsRemoved(Boolean.valueOf(isRemoved));
    }

    hibernateTemplate.update(comment);
    return comment;
  }

  @Override
  public String deleteComment(DoiBasedIdentity commentId) {
    Comment comment = getComment(commentId);
    String commentUri = comment.getCommentUri();
    hibernateTemplate.delete(comment);
    return commentUri;
  }

  private List<Flag> getCommentFlagsOn(Comment comment) {
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM Flag WHERE flaggedAnnotation = :comment");
      query.setParameter("comment", comment);
      return (List<Flag>) query.list();
    });
  }

  @Override
  public String removeFlagsFromComment(DoiBasedIdentity commentId) {
    Comment comment = getComment(commentId);
    String annotationUri = comment.getCommentUri();
    List<Flag> flags = getCommentFlagsOn(comment);
    hibernateTemplate.deleteAll(flags);
    return annotationUri;
  }

  @Override
  public Flag createCommentFlag(DoiBasedIdentity commentId, CommentFlagInputView input) {
    Comment comment = getComment(commentId);
    Long flagCreator = Long.valueOf(input.getCreatorUserId());

    Flag flag = new Flag();
    flag.setFlaggedComment(comment);
    flag.setUserProfileId(flagCreator);
    flag.setComment(input.getBody());
    flag.setReason(FlagReasonCode.fromString(input.getReasonCode()));
    flag.setLastModified(Date.from(Instant.now()));

    hibernateTemplate.save(flag);
    return flag;
  }

  @Override
  public Transceiver readAllCommentFlags() {
    return new Transceiver() {
      @Override
      protected List<CommentFlagOutputView> getData() throws IOException {
        CommentNodeView.Factory viewFactory = new CommentNodeView.Factory(runtimeConfiguration);
        return getAllFlags().stream().map(viewFactory::createFlagView).collect(Collectors.toList());
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  private List<Flag> getAllFlags() {
    return (List<Flag>) hibernateTemplate
        .execute(session -> session.createCriteria(Flag.class).list());
  }

  @Override
  public Transceiver readCommentFlag(String flagId) {
    return new EntityTransceiver<Flag>() {
      @Override
      protected Flag fetchEntity() {
        return getFlag(flagId);
      }

      @Override
      protected CommentFlagOutputView getView(Flag flag) {
        return new CommentNodeView.Factory(runtimeConfiguration).createFlagView(flag);
      }
    };
  }

  @Override
  public Transceiver readCommentFlagsOn(DoiBasedIdentity commentId) {
    return new Transceiver() {
      @Override
      protected Object getData() throws IOException {
        List<Flag> flags = getCommentFlagsOn(getComment(commentId));
        CommentNodeView.Factory viewFactory = new CommentNodeView.Factory(runtimeConfiguration);
        return flags.stream().map(viewFactory::createFlagView).collect(Collectors.toList());
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  @Override
  public String deleteCommentFlag(String flagId) {
    Flag flag = getFlag(flagId);
    hibernateTemplate.delete(flag);
    return flagId;
  }

  @Override
  public Transceiver readFlaggedComments() throws IOException {
    return new Transceiver() {
      @Override
      protected List<CommentNodeView> getData() throws IOException {
        CommentNodeView.Factory viewFactory = new CommentNodeView.Factory(runtimeConfiguration);
        return getAllFlags().stream()
            .map(Flag::getFlaggedComment)
            .map(viewFactory::create)
            .collect(Collectors.toCollection(ArrayList::new));
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  @Override
  public Transceiver readRecentComments(String journalKey, OptionalInt limit) {
    return new Transceiver() {
      @Override
      protected List<CommentNodeView> getData() throws IOException {
        List<Object[]> results = (List<Object[]>) hibernateTemplate.execute(session -> {
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
        CommentNodeView.Factory viewFactory = new CommentNodeView.Factory(runtimeConfiguration);
        return results.stream()
            .map((Object[] result) -> {
              Comment comment = (Comment) result[0];
              String articleDoi = (String) result[1];
              String articleTitle = (String) result[2];
              return viewFactory.create(comment, journalKey, articleDoi, articleTitle);
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
  public Transceiver getCommentCount(ArticleTable article) {
    return new Transceiver() {
      @Override
      protected Map<String, Object> getData() throws IOException {
        Map<String, Object> result = (Map<String, Object>) hibernateTemplate.execute(session -> {
          Query query = session.createQuery("" +
              "SELECT NEW MAP(COUNT(*) AS all, " +
              "COALESCE(SUM(CASE WHEN ann.parentID IS NULL THEN 1 ELSE 0 END), 0) AS root) " +
              "FROM Annotation ann " +
              "WHERE ann.articleID = :articleID ");
          query.setParameter("articleID", article.getArticleId());
          return query.uniqueResult();
        });
        return result;
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

}
