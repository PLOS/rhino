/*
 * Copyright (c) 2017 Public Library of Science
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
import org.ambraproject.rhino.rest.response.CacheableResponse;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.CommentCrudService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.view.comment.CommentCountView;
import org.ambraproject.rhino.view.comment.CommentFlagInputView;
import org.ambraproject.rhino.view.comment.CommentFlagOutputView;
import org.ambraproject.rhino.view.comment.CommentInputView;
import org.ambraproject.rhino.view.comment.CommentNodeView;
import org.ambraproject.rhino.view.comment.CommentOutputView;
import org.ambraproject.rhino.view.comment.CompetingInterestPolicy;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
  private Collection<Comment> fetchAllComments(Article article) {
    return (Collection<Comment>) hibernateTemplate.find("FROM Comment WHERE articleId = ?", article.getArticleId());
  }

  @Override
  public ServiceResponse<List<CommentOutputView>> serveComments(ArticleIdentifier articleId) throws IOException {
    Article article = articleCrudService.readArticle(articleId);
    Collection<Comment> comments = fetchAllComments(article);
    CommentOutputView.Factory factory
        = new CommentOutputView.Factory(new CompetingInterestPolicy(runtimeConfiguration),
        comments, article);
    List<CommentOutputView> views = comments.stream()
        .filter(comment -> comment.getParent() == null)
        .sorted(CommentOutputView.BY_DATE)
        .map(factory::buildView)
        .collect(Collectors.toList());
    return ServiceResponse.serveView(views);
  }

  private CommentOutputView createView(Comment comment) {
    Article article = comment.getArticle();
    Collection<Comment> articleComments = fetchAllComments(article);
    return new CommentOutputView.Factory(new CompetingInterestPolicy(runtimeConfiguration),
        articleComments, article).buildView(comment);
  }

  @Override
  public ServiceResponse<CommentOutputView> serveComment(CommentIdentifier commentId) throws IOException {
    Comment comment = readComment(commentId);
    return ServiceResponse.serveView(createView(comment));
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
  public ServiceResponse<CommentOutputView> createComment(Optional<ArticleIdentifier> articleId, CommentInputView input) {
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
    CompetingInterestPolicy competingInterestPolicy = new CompetingInterestPolicy(runtimeConfiguration);
    CommentOutputView.Factory viewFactory = new CommentOutputView.Factory(competingInterestPolicy,
        childComments, article);
    CommentOutputView view = viewFactory.buildView(created);
    return ServiceResponse.reportCreated(view);
  }

  @Override
  public ServiceResponse<CommentOutputView> patchComment(CommentIdentifier commentId, CommentInputView input) {
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

    return ServiceResponse.serveView(createView(comment));
  }

  @Override
  public String deleteComment(CommentIdentifier commentId) {
    Comment comment = readComment(commentId);
    hibernateTemplate.delete(comment);
    return commentId.getDoiName();
  }

  private Collection<Flag> getCommentFlagsOn(Comment comment) {
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM Flag WHERE flaggedComment = :comment");
      query.setParameter("comment", comment);
      return (Collection<Flag>) query.list();
    });
  }

  @Override
  public String removeFlagsFromComment(CommentIdentifier commentId) {
    Comment comment = readComment(commentId);
    Collection<Flag> flags = getCommentFlagsOn(comment);
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
  public ServiceResponse<Collection<CommentFlagOutputView>> readAllCommentFlags() {
    Collection<CommentFlagOutputView> views = getAllFlags().stream().map(commentNodeViewFactory::createFlagView)
        .collect(Collectors.toList());
    return ServiceResponse.serveView(views);
  }

  private List<Flag> getAllFlags() {
    return (List<Flag>) hibernateTemplate
        .execute(session -> session.createCriteria(Flag.class).list());
  }

  private Collection<Flag> getAllFlags(String journalKey) {
    Journal journal = journalCrudService.readJournal(journalKey);
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("SELECT DISTINCT f FROM ArticleIngestion i, Flag f " +
          "WHERE f.flaggedComment.article = i.article " +
          "AND i.journal = :journal ");
      query.setParameter("journal", journal);
      return (Collection<Flag>) query.list();
    });
  }

  @Override
  public CacheableResponse<CommentFlagOutputView> readCommentFlag(long flagId) {
    Flag flag = getFlag(flagId);
    return CacheableResponse.serveEntity(flag,
        f -> new CommentNodeView.Factory(runtimeConfiguration).createFlagView(f));
  }

  @Override
  public ServiceResponse<List<CommentFlagOutputView>> readCommentFlagsOn(CommentIdentifier commentId) {
    Collection<Flag> flags = getCommentFlagsOn(readComment(commentId));
    List<CommentFlagOutputView> views = flags.stream().map(commentNodeViewFactory::createFlagView).collect(Collectors.toList());
    return ServiceResponse.serveView(views);
  }

  @Override
  public ServiceResponse<Collection<CommentFlagOutputView>> readCommentFlagsForJournal(String journalKey) {
    Collection<Flag> flags = getAllFlags(journalKey);
    Collection<CommentFlagOutputView> views = flags.stream().map(commentNodeViewFactory::createFlagView).collect(Collectors.toList());
    return ServiceResponse.serveView(views);
  }

  @Override
  public Long deleteCommentFlag(Long flagId) {
    Flag flag = getFlag(flagId);
    hibernateTemplate.delete(flag);
    return flagId;
  }

  @Override
  public ServiceResponse<Collection<CommentNodeView>> serveFlaggedComments() throws IOException {
    HibernateCallback<List<Comment>> hibernateCallback = session ->
        session.createQuery("SELECT DISTINCT flaggedComment FROM Flag").list();
    Collection<Comment> flaggedComments = hibernateTemplate.execute(hibernateCallback);
    Collection<CommentNodeView> views = flaggedComments.stream().map(commentNodeViewFactory::create).collect(Collectors.toList());
    return ServiceResponse.serveView(views);
  }

  private long getCount(Session session, Article article, String whereClause) {
    Query query = session.createQuery("SELECT COUNT(*) FROM Comment WHERE article = :article " + whereClause);
    query.setParameter("article", article);
    return ((Number) query.uniqueResult()).longValue();
  }

  @Override
  public ServiceResponse<CommentCountView> getCommentCount(Article article) {
    CommentCountView view = hibernateTemplate.execute((Session session) -> {
      long all = getCount(session, article, "AND isRemoved = FALSE");
      long root = getCount(session, article, "AND isRemoved = FALSE AND parent IS NULL");
      long removed = getCount(session, article, "AND isRemoved = TRUE");
      return new CommentCountView(all, root, removed);
    });
    return ServiceResponse.serveView(view);
  }

  @Override
  public ServiceResponse<Collection<CommentNodeView>> getCommentsCreatedOn(LocalDate date) {
    Collection<CommentNodeView> views = hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM Comment WHERE DATE(created) = :date");
      query.setParameter("date", java.sql.Date.valueOf(date));
      Collection<Comment> comments = query.list();

      return comments.stream().map(commentNodeViewFactory::create).collect(Collectors.toList());
    });
    return ServiceResponse.serveView(views);
  }
}
