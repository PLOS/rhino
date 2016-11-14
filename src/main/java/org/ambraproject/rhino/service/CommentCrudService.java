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
