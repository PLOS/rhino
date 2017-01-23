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

package org.ambraproject.rhino.service;

import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.CommentIdentifier;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.Comment;
import org.ambraproject.rhino.model.Flag;
import org.ambraproject.rhino.rest.response.CacheableResponse;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.view.comment.CommentCountView;
import org.ambraproject.rhino.view.comment.CommentFlagInputView;
import org.ambraproject.rhino.view.comment.CommentFlagOutputView;
import org.ambraproject.rhino.view.comment.CommentInputView;
import org.ambraproject.rhino.view.comment.CommentNodeView;
import org.ambraproject.rhino.view.comment.CommentOutputView;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service that handles comments and replies associated with articles. For legacy reasons, these are referred to
 * internally as "annotations," and they share a common implementation.
 */
public interface CommentCrudService {

  /**
   * Forwards all comments, and any replies associated with them, for a given article to the receiver.  The comments are
   * returned as a list.  Each comment has a "replies" list that contains any replies (recursively).
   *
   * @param articleId identifies the article
   * @throws IOException
   */
  public ServiceResponse<List<CommentOutputView>> serveComments(ArticleIdentifier articleId)
      throws IOException;

  /**
   * Reads the comment data for a single comment to the receiver.
   *
   * @param commentId identifies the comment
   * @throws IOException
   */
  public ServiceResponse<CommentOutputView> serveComment(CommentIdentifier commentId)
      throws IOException;

  /**
   * Reads the comment data for a list of flagged comments to the receiver.
   *
   * @throws IOException
   */
  public ServiceResponse<Collection<CommentNodeView>> serveFlaggedComments() throws IOException;

  public Optional<Comment> getComment(CommentIdentifier commentId);

  public ServiceResponse<CommentOutputView> createComment(Optional<ArticleIdentifier> articleId, CommentInputView input);

  public ServiceResponse<CommentOutputView> patchComment(CommentIdentifier commentId, CommentInputView input);

  public String deleteComment(CommentIdentifier commentId);

  public String removeFlagsFromComment(CommentIdentifier commentId);

  public Flag createCommentFlag(CommentIdentifier commentId, CommentFlagInputView input);

  public ServiceResponse<Collection<CommentFlagOutputView>> readAllCommentFlags();

  public ServiceResponse<Collection<CommentFlagOutputView>> readCommentFlagsForJournal(String journalKey);

  public CacheableResponse<CommentFlagOutputView> readCommentFlag(long flagId);

  public ServiceResponse<List<CommentFlagOutputView>> readCommentFlagsOn(CommentIdentifier commentId);

  public Long deleteCommentFlag(Long flagId);

  public ServiceResponse<CommentCountView> getCommentCount(Article article);

  public ServiceResponse<Collection<CommentNodeView>> getCommentsCreatedOn(LocalDate date);
}
