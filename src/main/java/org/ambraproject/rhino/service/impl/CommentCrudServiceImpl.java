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
import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.CommentIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.Comment;
import org.ambraproject.rhino.model.Flag;
import org.ambraproject.rhino.model.FlagReasonCode;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.CommentCrudService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.util.response.EntityTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.comment.CommentCountView;
import org.ambraproject.rhino.view.comment.CommentFlagInputView;
import org.ambraproject.rhino.view.comment.CommentFlagOutputView;
import org.ambraproject.rhino.view.comment.CommentInputView;
import org.ambraproject.rhino.view.comment.CommentNodeView;
import org.ambraproject.rhino.view.comment.CommentOutputView;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("JpaQlInspection")
public class CommentCrudServiceImpl extends AmbraService implements CommentCrudService {

  @Autowired
  private CommentNodeView.Factory commentNodeViewFactory;
  @Autowired
  private JournalCrudService journalCrudService;
  @Autowired
  private ArticleCrudService articleCrudService;

  /**
   * Fetch all annotations that belong to an article.
   *
   * @param article the article
   * @return the collection of annotations
   */
  private List<Comment> fetchAllComments(Article article) {
    return (List<Comment>) hibernateTemplate.find("FROM Comment WHERE articleId = ?", article.getArticleId());
  }

  @Override
  public Transceiver serveComments(ArticleIdentifier articleId) throws IOException {
    return new Transceiver() {
      @Override
      protected Collection<CommentOutputView> getData() throws IOException {
        Article article = articleCrudService.readArticle(articleId);
        List<Comment> comments = fetchAllComments(article);
        CommentOutputView.Factory factory
            = new CommentOutputView.Factory(runtimeConfiguration, comments);
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
  public Transceiver serveComment(CommentIdentifier commentId) throws IOException {
    return new Transceiver() {
      @Override
      protected CommentOutputView getData() throws IOException {
        Comment comment = readComment(commentId);
        return new CommentOutputView.Factory(runtimeConfiguration,
            fetchAllComments(comment.getArticle())).buildView(comment);
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  @Override
  public Optional<Comment> getComment(CommentIdentifier commentId) {
    return Optional.ofNullable(hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM Comment WHERE commentUri = :commentUri");
      query.setParameter("commentUri", commentId.getDoiName());
      return (Comment) query.uniqueResult();
    }));
  }

  private Comment readComment(CommentIdentifier commentId) {
    return getComment(commentId).orElseThrow(() ->
        new RestClientException("comment not found with URI: " + commentId, HttpStatus.NOT_FOUND));
  }

  private Flag getFlag(Long commentFlagId) {
    Flag flag = hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM Flag WHERE commentFlagId = :commentFlagId");
      query.setParameter("commentFlagId", commentFlagId);
      return (Flag) query.uniqueResult();
    });
    if (flag == null) {
      String message = "Comment flag not found at the provided ID: " + commentFlagId;
      throw new RestClientException(message, HttpStatus.NOT_FOUND);
    }
    return flag;
  }

  private static final Pattern DOI_PREFIX_PATTERN = Pattern.compile("^\\d+\\.\\d+/");

  private static String extractDoiPrefix(ArticleIdentifier doi) {
    Matcher matcher = DOI_PREFIX_PATTERN.matcher(doi.getDoiName());
    Preconditions.checkArgument(matcher.find());
    return matcher.group();
  }

  @Override
  public CommentOutputView createComment(Optional<ArticleIdentifier> articleId, CommentInputView input) {
    final Optional<String> parentCommentUri = Optional.ofNullable(input.getParentCommentId());

    final Article article;
    final Comment parentComment;
    if (parentCommentUri.isPresent()) {
      parentComment = readComment(CommentIdentifier.create(parentCommentUri.get()));
      if (parentComment == null) {
        throw new RestClientException("Parent comment not found: " + parentCommentUri, HttpStatus.BAD_REQUEST);
      }

      article = parentComment.getArticle();

      ArticleIdentifier articleDoiFromDb = ArticleIdentifier.create(parentComment.getArticle().getDoi());
      if (!articleId.isPresent()) {
        articleId = Optional.of(articleDoiFromDb);
      } else if (!articleId.get().equals(articleDoiFromDb)) {
        String message = String.format("Parent comment (%s) not from declared article (%s).",
            parentCommentUri.get(), articleId.get());
        throw new RestClientException(message, HttpStatus.BAD_REQUEST);
      }
    } else {
      // The comment is a root-level reply to an article (no parent comment).
      if (!articleId.isPresent()) {
        throw new RestClientException("Must provide articleId or parentCommentUri", HttpStatus.BAD_REQUEST);
      }

      article = articleCrudService.readArticle(articleId.get());
      parentComment = null;
    }

    String doiPrefix = extractDoiPrefix(articleId.get()); // comment receives same DOI prefix as article
    UUID uuid = UUID.randomUUID(); // generate a new DOI out of a random UUID
    Doi createdCommentUri = Doi.create(doiPrefix + "annotation/" + uuid);

    Comment created = new Comment();
    created.setArticle(article);
    created.setParent(parentComment);
    created.setCommentUri(createdCommentUri.getName());
    created.setUserProfileID(Long.valueOf(Strings.nullToEmpty(input.getCreatorUserId())));
    created.setTitle(Strings.nullToEmpty(input.getTitle()));
    created.setBody(Strings.nullToEmpty(input.getBody()));
    created.setHighlightedText(Strings.nullToEmpty(input.getHighlightedText()));
    created.setCompetingInterestBody(Strings.nullToEmpty(input.getCompetingInterestStatement()));
    created.setIsRemoved(Boolean.valueOf(Strings.nullToEmpty(input.getIsRemoved())));

    hibernateTemplate.save(created);

    List<Comment> childComments = ImmutableList.of(); // the new comment can't have any children yet
    CommentOutputView.Factory viewFactory
        = new CommentOutputView.Factory(runtimeConfiguration, childComments);
    return viewFactory.buildView(created);
  }

  @Override
  public CommentOutputView patchComment(CommentIdentifier commentId, CommentInputView input) {
    Comment comment = readComment(commentId);

    String declaredUri = input.getAnnotationUri();
    if (declaredUri != null && !CommentIdentifier.create(declaredUri).equals(commentId)) {
      throw new RestClientException("Mismatched commentUri in body", HttpStatus.BAD_REQUEST);
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

    return new CommentOutputView.Factory(runtimeConfiguration,
        fetchAllComments(comment.getArticle())).buildView(comment);
  }

  @Override
  public String deleteComment(CommentIdentifier commentId) {
    Comment comment = readComment(commentId);
    hibernateTemplate.delete(comment);
    return commentId.getDoiName();
  }

  private List<Flag> getCommentFlagsOn(Comment comment) {
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM Flag WHERE flaggedComment = :comment");
      query.setParameter("comment", comment);
      return (List<Flag>) query.list();
    });
  }

  @Override
  public String removeFlagsFromComment(CommentIdentifier commentId) {
    Comment comment = readComment(commentId);
    List<Flag> flags = getCommentFlagsOn(comment);
    hibernateTemplate.deleteAll(flags);
    return commentId.getDoiName();
  }

  @Override
  public Flag createCommentFlag(CommentIdentifier commentId, CommentFlagInputView input) {
    Comment comment = readComment(commentId);
    Long flagCreator = Long.valueOf(input.getCreatorUserId());

    Flag flag = new Flag();
    flag.setFlaggedComment(comment);
    flag.setUserProfileId(flagCreator);
    flag.setComment(input.getBody());
    flag.setReason(FlagReasonCode.fromString(input.getReasonCode()));

    hibernateTemplate.save(flag);
    return flag;
  }

  @Override
  public Transceiver readAllCommentFlags() {
    return new Transceiver() {
      @Override
      protected List<CommentFlagOutputView> getData() throws IOException {
        return getAllFlags().stream().map(commentNodeViewFactory::createFlagView)
            .collect(Collectors.toList());
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
  public Transceiver readCommentFlag(Long flagId) {
    return new EntityTransceiver<Flag>() {

      @Override
      protected Flag fetchEntity() {
        return getFlag(flagId);
      }

      @Override
      protected Object getView(Flag flag) {
        return new CommentNodeView.Factory(runtimeConfiguration).createFlagView(flag);
      }
    };
  }

  @Override
  public Transceiver readCommentFlagsOn(CommentIdentifier commentId) {
    return new Transceiver() {
      @Override
      protected Object getData() throws IOException {
        List<Flag> flags = getCommentFlagsOn(readComment(commentId));
        return flags.stream().map(commentNodeViewFactory::createFlagView).collect(Collectors.toList());
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  @Override
  public Long deleteCommentFlag(Long flagId) {
    Flag flag = getFlag(flagId);
    hibernateTemplate.delete(flag);
    return flagId;
  }

  private List<CommentNodeView> readFlaggedComments(HibernateCallback<List<Comment>> hibernateCallback) {
    List<Comment> flaggedComments = hibernateTemplate.execute(hibernateCallback);
    return flaggedComments.stream().map(commentNodeViewFactory::create).collect(Collectors.toList());
  }

  @Override
  public Transceiver serveFlaggedComments() throws IOException {
    return new Transceiver() {
      @Override
      protected List<CommentNodeView> getData() throws IOException {
        return readFlaggedComments(session ->
            session.createQuery("SELECT DISTINCT flaggedComment FROM Flag").list());
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  @Override
  public Transceiver serveFlaggedComments(String journalKey) throws IOException {
    return new Transceiver() {
      @Override
      protected List<CommentNodeView> getData() throws IOException {
        Journal journal = journalCrudService.readJournal(journalKey);
        return readFlaggedComments(session -> {
          Query query = session.createQuery("" +
              "SELECT DISTINCT f.flaggedComment " +
              "FROM Flag f, Article a " +
              "WHERE f.flaggedComment.articleID = a.ID " +
              "  AND :journal IN ELEMENTS(a.journals)");
          query.setParameter("journal", journal);
          return query.list();
        });
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
      //todo: include article title?
      @Override
      protected List<CommentNodeView> getData() throws IOException {
        List<Object[]> results = (List<Object[]>) hibernateTemplate.execute(session -> {
          Query query = session.createQuery("" +
              "SELECT com, art.doi " +
              "FROM Comment com, Article art, Journal j " +
              "WHERE com.article = art " +
              "  AND j IN ELEMENTS(art.journals) " +
              "  AND j.journalKey = :journalKey " +
              "ORDER BY com.created DESC");
          query.setParameter("journalKey", journalKey);
          limit.ifPresent(query::setMaxResults);
          return query.list();
        });
        return results.stream()
            .map((Object[] result) -> {
              Comment comment = (Comment) result[0];
              String articleDoi = (String) result[1];
              return commentNodeViewFactory.create(comment, articleDoi);
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
  public Transceiver getCommentCount(Article article) {
    return new Transceiver() {
      private long getCount(Session session, String whereClause) {
        Query query = session.createQuery("SELECT COUNT(*) FROM Comment WHERE article = :article " + whereClause);
        query.setParameter("article", article);
        return ((Number) query.uniqueResult()).longValue();
      }

      @Override
      protected CommentCountView getData() throws IOException {
        return hibernateTemplate.execute((Session session) -> {
          long all = getCount(session, "AND isRemoved = FALSE");
          long root = getCount(session, "AND isRemoved = FALSE AND parent IS NULL");
          long removed = getCount(session, "AND isRemoved = TRUE");
          return new CommentCountView(all, root, removed);
        });
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  @Override
  public List<CommentNodeView> getCommentsCreatedOn(LocalDate date) {
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM Comment WHERE DATE(created) = :date");
      query.setParameter("date", java.sql.Date.valueOf(date));
      List<Comment> comments = query.list();

      return comments.stream().map(commentNodeViewFactory::create).collect(Collectors.toList());
    });
  }
}
