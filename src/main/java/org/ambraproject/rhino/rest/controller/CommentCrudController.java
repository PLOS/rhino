package org.ambraproject.rhino.rest.controller;

import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.CommentIdentifier;
import org.ambraproject.rhino.model.Flag;
import org.ambraproject.rhino.rest.DoiEscaping;
import org.ambraproject.rhino.service.CommentCrudService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.comment.CommentFlagInputView;
import org.ambraproject.rhino.view.comment.CommentInputView;
import org.ambraproject.rhino.view.comment.CommentNodeView;
import org.ambraproject.rhino.view.comment.CommentOutputView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

@Controller
public class CommentCrudController extends RestController {

  @Autowired
  private CommentNodeView.Factory commentNodeViewFactory;
  @Autowired
  private CommentCrudService commentCrudService;

  @RequestMapping(value = "/comments/{commentDoi:.+}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("commentDoi") String commentDoi)
      throws IOException {
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));

    // TODO: Look up article; redirect to main service
    // TODO: Equivalent aliases for other methods?
  }

  @RequestMapping(value = "/articles/{articleDoi}/comments/{commentDoi:.+}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("articleDoi") String articleDoi,
                   @PathVariable("commentDoi") String commentDoi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(articleDoi));
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleId

    commentCrudService.serveComment(commentId).respond(request, response, entityGson);
  }

  @RequestMapping(value = "/comments", method = RequestMethod.GET, params = {"flagged"})
  public void readAllFlaggedComments(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    commentCrudService.serveFlaggedComments().respond(request, response, entityGson);
  }

  @RequestMapping(value = "/comments", method = RequestMethod.GET, params = {"flagged", "journal"})
  public void readFlaggedComments(HttpServletRequest request, HttpServletResponse response,
                                  @RequestParam("journal") String journalKey)
      throws IOException {
    commentCrudService.serveFlaggedComments(journalKey).respond(request, response, entityGson);
  }

  /**
   * Provides a utility endpoint for the publication workflow.
   * The main use-case is to notify journal stakeholders when new comments are posted.
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = "/comments", method = RequestMethod.GET, params = "created")
  public void getCommentsCreatedOn(HttpServletRequest request, HttpServletResponse response,
                                   @ApiParam(value = "Date Format: yyyy-MM-dd")
                                   @RequestParam(value = "date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date)
      throws IOException {

    Transceiver.serveUntimestampedView(() -> commentCrudService.getCommentsCreatedOn(date))
        .respond(request, response, entityGson);
  }

  @RequestMapping(value = "/commentFlags", method = RequestMethod.GET)
  public void readAllFlags(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    commentCrudService.readAllCommentFlags().respond(request, response, entityGson);
  }

  @RequestMapping(value = "/articles/{articleDoi}/comments", method = RequestMethod.POST)
  public ResponseEntity<?> create(HttpServletRequest request,
                                  @PathVariable("articleDoi") String articleDoi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(articleDoi));
    CommentInputView input = readJsonFromRequest(request, CommentInputView.class);

    // TODO: Pass Optional.empty() if client POSTed to "/comments"?
    Optional<ArticleIdentifier> articleIdObj = Optional.of(articleId);

    CommentOutputView created = commentCrudService.createComment(articleIdObj, input);
    return reportCreated(created);
  }

  @RequestMapping(value = "/articles/{articleDoi}/comments/{commentDoi}/flags", method = RequestMethod.DELETE)
  public ResponseEntity<?> removeAllFlags(HttpServletRequest request,
                                          @PathVariable("articleDoi") String articleDoi,
                                          @PathVariable("commentDoi") String commentDoi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(articleDoi));
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleId

    commentCrudService.removeFlagsFromComment(commentId);
    return reportDeleted(commentId.toString());
  }

  @RequestMapping(value = "/articles/{articleDoi}/comments/{commentDoi:.+}", method = RequestMethod.PATCH)
  public ResponseEntity<?> patch(HttpServletRequest request,
                                 @PathVariable("articleDoi") String articleDoi,
                                 @PathVariable("commentDoi") String commentDoi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(articleDoi));
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleId

    CommentInputView input = readJsonFromRequest(request, CommentInputView.class);
    CommentOutputView patched = commentCrudService.patchComment(commentId, input);
    return reportUpdated(patched);
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
  public ResponseEntity<String> createFlag(HttpServletRequest request,
                                           @PathVariable("articleDoi") String articleDoi,
                                           @PathVariable("commentDoi") String commentDoi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(articleDoi));
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleId

    CommentFlagInputView input = readJsonFromRequest(request, CommentFlagInputView.class);
    Flag commentFlag = commentCrudService.createCommentFlag(commentId, input);
    return reportCreated(commentNodeViewFactory.createFlagView(commentFlag));
  }

  @RequestMapping(value = "/articles/{articleDoi}/comments/{commentDoi}/flags", method = RequestMethod.GET)
  public void readFlagsOnComment(HttpServletRequest request, HttpServletResponse response,
                                 @PathVariable("articleDoi") String articleDoi,
                                 @PathVariable("commentDoi") String commentDoi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(articleDoi));
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleId

    commentCrudService.readCommentFlagsOn(commentId).respond(request, response, entityGson);
  }

  @RequestMapping(value = "/articles/{articleDoi}/comments/{commentDoi}/flags/{flagId}", method = RequestMethod.GET)
  public void readFlag(HttpServletRequest request, HttpServletResponse response,
                       @PathVariable("articleDoi") String articleDoi,
                       @PathVariable("commentDoi") String commentDoi,
                       @PathVariable("flagId") long flagId)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(articleDoi));
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleId and commentId

    commentCrudService.readCommentFlag(flagId).respond(request, response, entityGson);
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
