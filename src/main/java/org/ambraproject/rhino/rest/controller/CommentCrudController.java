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

package org.ambraproject.rhino.rest.controller;

import com.wordnik.swagger.annotations.ApiImplicitParam;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.CommentIdentifier;
import org.ambraproject.rhino.model.Flag;
import org.ambraproject.rhino.rest.DoiEscaping;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.service.CommentCrudService;
import org.ambraproject.rhino.view.comment.CommentFlagInputView;
import org.ambraproject.rhino.view.comment.CommentInputView;
import org.ambraproject.rhino.view.comment.CommentNodeView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Date;
import java.util.Optional;

@Controller
public class CommentCrudController extends RestController {

  @Autowired
  private CommentNodeView.Factory commentNodeViewFactory;
  @Autowired
  private CommentCrudService commentCrudService;

  @RequestMapping(value = "/comments/{commentDoi:.+}", method = RequestMethod.GET)
  public ResponseEntity<?> read(@PathVariable("commentDoi") String commentDoi)
      throws IOException {
    // TODO: Consider: Look up article; redirect to main service
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    return commentCrudService.serveComment(commentId).asJsonResponse(entityGson);
  }

  @RequestMapping(value = "/articles/{articleDoi}/comments/{commentDoi:.+}", method = RequestMethod.GET)
  public ResponseEntity<?> read(@PathVariable("articleDoi") String articleDoi,
                                @PathVariable("commentDoi") String commentDoi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(articleDoi));
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleId

    return commentCrudService.serveComment(commentId).asJsonResponse(entityGson);
  }

  @RequestMapping(value = "/comments", method = RequestMethod.GET, params = {"flagged"})
  @ApiImplicitParam(name = "flagged", value = "flagged flag (any value)", required = true,
      defaultValue = "flagged", paramType = "query", dataType = "string")
  public ResponseEntity<?> readAllFlaggedComments() throws IOException {
    return commentCrudService.serveFlaggedComments().asJsonResponse(entityGson);
  }

  /**
   * Provides a utility endpoint for the publication workflow.
   * The main use-case is to notify journal stakeholders when new comments are posted.
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = "/comments", method = RequestMethod.GET, params = "created")
  public ResponseEntity<?> getCommentsCreatedOn(@ApiParam(value = "Date Format: yyyy-MM-dd")
                                                @RequestParam(value = "date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date)
      throws IOException {
    return commentCrudService.getCommentsCreatedOn(date).asJsonResponse(entityGson);
  }

  @RequestMapping(value = "/commentFlags", method = RequestMethod.GET)
  public ResponseEntity<?> readAllFlags()
      throws IOException {
    return commentCrudService.readAllCommentFlags().asJsonResponse(entityGson);
  }

  @RequestMapping(value = "/commentFlags", method = RequestMethod.GET, params = {"journal"})
  public ResponseEntity<?> readAllFlagsByJournal(@RequestParam("journal") String journalKey)
      throws IOException {
    return commentCrudService.readCommentFlagsForJournal(journalKey).asJsonResponse(entityGson);
  }

  @RequestMapping(value = "/articles/{articleDoi}/comments", method = RequestMethod.POST)
  @ApiImplicitParam(name = "body", paramType = "body", dataType = "CommentInputView",
      value = "example: {\"creatorUserId\": 10365, " +
          "\"parentCommentId\": \"10.1371/annotation/0043aae2-f69d-4a05-ab19-4709704eb749\", " +
          "\"title\": \"no, really watch this\", " +
          "\"body\": \"http://www.youtube.com/watch?v=iGQdwSAAz9I\", " +
          "\"highlightedText\": \"whoah...\", " +
          "\"competingInterestStatement\": \"I'm going for an Emmy\"}")
  public ResponseEntity<?> create(HttpServletRequest request,
                                  @PathVariable("articleDoi") String articleDoi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(articleDoi));
    CommentInputView input = readJsonFromRequest(request, CommentInputView.class);

    // TODO: Pass Optional.empty() if client POSTed to "/comments"?
    Optional<ArticleIdentifier> articleIdObj = Optional.of(articleId);

    return commentCrudService.createComment(articleIdObj, input).asJsonResponse(entityGson);
  }

  @RequestMapping(value = "/articles/{articleDoi}/comments/{commentDoi}/flags", method = RequestMethod.DELETE)
  public ResponseEntity<?> removeAllFlags(@PathVariable("articleDoi") String articleDoi,
                                          @PathVariable("commentDoi") String commentDoi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(articleDoi));
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleId

    commentCrudService.removeFlagsFromComment(commentId);
    return reportDeleted(commentId.toString());
  }

  @RequestMapping(value = "/articles/{articleDoi}/comments/{commentDoi:.+}", method = RequestMethod.PATCH)
  @ApiImplicitParam(name = "body", paramType = "body", dataType = "CommentInputView",
      value= "example #1: {\"title\": \"new title\"}<br>" +
          "example #2: {\"body\": \"comment body replacement text\"}<br>" +
          "example #3: {\"isRemoved\": \"true\"}")
  public ResponseEntity<?> patch(HttpServletRequest request,
                                 @PathVariable("articleDoi") String articleDoi,
                                 @PathVariable("commentDoi") String commentDoi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(articleDoi));
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleId

    CommentInputView input = readJsonFromRequest(request, CommentInputView.class);
    return commentCrudService.patchComment(commentId, input).asJsonResponse(entityGson);
  }

  @RequestMapping(value = "/articles/{articleDoi}/comments/{commentDoi:.+}", method = RequestMethod.DELETE)
  @ApiOperation(value = "delete", notes = "Performs a hard delete operation in the database. " +
      "NOTE: fails loudly if attempting to delete a comment that has any replies. All replies must " +
      "be deleted first.")
  public ResponseEntity<?> delete(HttpServletRequest request,
                                  @PathVariable("articleDoi") String articleDoi,
                                  @PathVariable("commentDoi") String commentDoi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(articleDoi));
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleId

    String deletedCommentUri = commentCrudService.deleteComment(commentId);
    return reportDeleted(deletedCommentUri);
  }

  @RequestMapping(value = "/articles/{articleDoi}/comments/{commentDoi}/flags", method = RequestMethod.POST)
  @ApiImplicitParam(name = "body", paramType = "body", dataType = "CommentFlagInputView",
      value = "example: {\"creatorUserId\": 10365, \"body\": \"oops\", \"reasonCode\": \"spam\"}")
  public ResponseEntity<?> createFlag(HttpServletRequest request,
                                      @PathVariable("articleDoi") String articleDoi,
                                      @PathVariable("commentDoi") String commentDoi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(articleDoi));
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleId

    CommentFlagInputView input = readJsonFromRequest(request, CommentFlagInputView.class);
    Flag commentFlag = commentCrudService.createCommentFlag(commentId, input);
    return ServiceResponse.reportCreated(commentNodeViewFactory.createFlagView(commentFlag)).asJsonResponse(entityGson);
  }

  @RequestMapping(value = "/articles/{articleDoi}/comments/{commentDoi}/flags", method = RequestMethod.GET)
  public void readFlagsOnComment(HttpServletRequest request, HttpServletResponse response,
                                 @PathVariable("articleDoi") String articleDoi,
                                 @PathVariable("commentDoi") String commentDoi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(articleDoi));
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleId

    commentCrudService.readCommentFlagsOn(commentId).asJsonResponse(entityGson);
  }

  @RequestMapping(value = "/articles/{articleDoi}/comments/{commentDoi}/flags/{flagId}", method = RequestMethod.GET)
  public void readFlag(@RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) Date ifModifiedSince,
                       @PathVariable("articleDoi") String articleDoi,
                       @PathVariable("commentDoi") String commentDoi,
                       @PathVariable("flagId") long flagId)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(articleDoi));
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleId and commentId

    commentCrudService.readCommentFlag(flagId).getIfModified(ifModifiedSince).asJsonResponse(entityGson);
  }

  @RequestMapping(value = "/articles/{articleDoi}/comments/{commentDoi}/flags/{flagId}", method = RequestMethod.DELETE)
  public ResponseEntity<Object> removeFlag(@PathVariable("articleDoi") String articleDoi,
                                           @PathVariable("commentDoi") String commentDoi,
                                           @PathVariable("flagId") long flagId)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(articleDoi));
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleId and commentId

    commentCrudService.deleteCommentFlag(flagId);
    return reportDeleted(Long.toString(flagId));
  }

}
