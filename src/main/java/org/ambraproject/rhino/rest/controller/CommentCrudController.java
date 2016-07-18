package org.ambraproject.rhino.rest.controller;

import com.wordnik.swagger.annotations.ApiOperation;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.identity.CommentIdentifier;
import org.ambraproject.rhino.model.Comment;
import org.ambraproject.rhino.model.Flag;
import org.ambraproject.rhino.rest.DoiEscaping;
import org.ambraproject.rhino.service.CommentCrudService;
import org.ambraproject.rhino.view.comment.CommentFlagInputView;
import org.ambraproject.rhino.view.comment.CommentInputView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class CommentCrudController extends RestController {

  @Autowired
  private CommentCrudService commentCrudService;

  @RequestMapping(value = "/comments/{commentDoi:.+}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("commentDoi") String commentDoi)
      throws IOException {
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));

    // TODO: Look up article revision; redirect to main service
    // TODO: Equivalent aliases for other methods?
  }

  @RequestMapping(value = "/articles/{articleDoi}/revisions/{number}/comments/{commentDoi:.+}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("articleDoi") String articleDoi,
                   @PathVariable("number") int revisionNumber,
                   @PathVariable("commentDoi") String commentDoi)
      throws IOException {
    ArticleRevisionIdentifier articleRevisionId = ArticleRevisionIdentifier.create(
        DoiEscaping.unescape(articleDoi), revisionNumber);
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleRevisionId

    commentCrudService.readComment(commentId).respond(request, response, entityGson);
  }

  @RequestMapping(value = "/comments/", method = RequestMethod.GET, params = {"flagged"})
  public void readAllFlaggedComments(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    commentCrudService.readFlaggedComments().respond(request, response, entityGson);
  }

  @RequestMapping(value = "/commentFlags/", method = RequestMethod.GET)
  public void readAllFlags(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    commentCrudService.readAllCommentFlags().respond(request, response, entityGson);
  }

  @RequestMapping(value = "/articles/{articleDoi}/revisions/{number}", method = RequestMethod.POST)
  public ResponseEntity<?> create(HttpServletRequest request,
                                  @PathVariable("articleDoi") String articleDoi,
                                  @PathVariable("number") int revisionNumber)
      throws IOException {
    ArticleRevisionIdentifier articleRevisionId = ArticleRevisionIdentifier.create(
        DoiEscaping.unescape(articleDoi), revisionNumber);
    CommentInputView input = readJsonFromRequest(request, CommentInputView.class);
    Comment created = commentCrudService.createComment(articleRevisionId, input);
    return reportCreated(created.getCommentUri());
  }

  @RequestMapping(value = "/articles/{articleDoi}/revisions/{number}/comments/{commentDoi}/flags", method = RequestMethod.DELETE)
  public ResponseEntity<?> removeAllFlags(HttpServletRequest request,
                                          @PathVariable("articleDoi") String articleDoi,
                                          @PathVariable("number") int revisionNumber,
                                          @PathVariable("commentDoi") String commentDoi)
      throws IOException {
    ArticleRevisionIdentifier articleRevisionId = ArticleRevisionIdentifier.create(
        DoiEscaping.unescape(articleDoi), revisionNumber);
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleRevisionId

    commentCrudService.removeFlagsFromComment(commentId);
    return reportOk(commentId.toString());
  }

  @RequestMapping(value = "/articles/{articleDoi}/revisions/{number}/comments/{commentDoi:.+}", method = RequestMethod.PATCH)
  public ResponseEntity<?> patch(HttpServletRequest request,
                                 @PathVariable("articleDoi") String articleDoi,
                                 @PathVariable("number") int revisionNumber,
                                 @PathVariable("commentDoi") String commentDoi)
      throws IOException {
    ArticleRevisionIdentifier articleRevisionId = ArticleRevisionIdentifier.create(
        DoiEscaping.unescape(articleDoi), revisionNumber);
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleRevisionId

    CommentInputView input = readJsonFromRequest(request, CommentInputView.class);
    Comment patched = commentCrudService.patchComment(commentId, input);
    return reportOk(patched.getCommentUri());
  }

  @RequestMapping(value = "/articles/{articleDoi}/revisions/{number}/comments/{commentDoi:.+}", method = RequestMethod.DELETE)
  @ApiOperation(value = "delete", notes = "Performs a hard delete operation in the database. " +
      "NOTE: fails loudly if attempting to delete a comment that has any replies. All replies must " +
      "be deleted first.")
  public ResponseEntity<?> delete(HttpServletRequest request,
                                  @PathVariable("articleDoi") String articleDoi,
                                  @PathVariable("number") int revisionNumber,
                                  @PathVariable("commentDoi") String commentDoi)
      throws IOException {
    ArticleRevisionIdentifier articleRevisionId = ArticleRevisionIdentifier.create(
        DoiEscaping.unescape(articleDoi), revisionNumber);
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleRevisionId

    String deletedCommentUri = commentCrudService.deleteComment(commentId);
    return reportOk(deletedCommentUri);
  }

  @RequestMapping(value = "/articles/{articleDoi}/revisions/{number}/comments/{commentDoi}/flags", method = RequestMethod.POST)
  public ResponseEntity<String> createFlag(HttpServletRequest request,
                                           @PathVariable("articleDoi") String articleDoi,
                                           @PathVariable("number") int revisionNumber,
                                           @PathVariable("commentDoi") String commentDoi)
      throws IOException {
    ArticleRevisionIdentifier articleRevisionId = ArticleRevisionIdentifier.create(
        DoiEscaping.unescape(articleDoi), revisionNumber);
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleRevisionId

    CommentFlagInputView input = readJsonFromRequest(request, CommentFlagInputView.class);
    Flag commentFlag = commentCrudService.createCommentFlag(commentId, input);
    return reportCreated(commentFlag.getCommentFlagId().toString());
  }

  @RequestMapping(value = "/articles/{articleDoi}/revisions/{number}/comments/{commentDoi}/flags", method = RequestMethod.GET)
  public void readFlagsOnComment(HttpServletRequest request, HttpServletResponse response,
                                 @PathVariable("articleDoi") String articleDoi,
                                 @PathVariable("number") int revisionNumber,
                                 @PathVariable("commentDoi") String commentDoi)
      throws IOException {
    ArticleRevisionIdentifier articleRevisionId = ArticleRevisionIdentifier.create(
        DoiEscaping.unescape(articleDoi), revisionNumber);
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleRevisionId

    commentCrudService.readCommentFlagsOn(commentId).respond(request, response, entityGson);
  }

  @RequestMapping(value = "/articles/{articleDoi}/revisions/{number}/comments/{commentDoi}/flags/{flagId}", method = RequestMethod.GET)
  public void readFlag(HttpServletRequest request, HttpServletResponse response,
                       @PathVariable("articleDoi") String articleDoi,
                       @PathVariable("number") int revisionNumber,
                       @PathVariable("commentDoi") String commentDoi,
                       @PathVariable("flagId") long flagId)
      throws IOException {
    ArticleRevisionIdentifier articleRevisionId = ArticleRevisionIdentifier.create(
        DoiEscaping.unescape(articleDoi), revisionNumber);
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleRevisionId and commentId

    commentCrudService.readCommentFlag(flagId).respond(request, response, entityGson);
  }

  @RequestMapping(value = "/articles/{articleDoi}/revisions/{number}/comments/{commentDoi}/flags/{flagId}", method = RequestMethod.DELETE)
  public ResponseEntity<Object> removeFlag(@PathVariable("articleDoi") String articleDoi,
                                           @PathVariable("number") int revisionNumber,
                                           @PathVariable("commentDoi") String commentDoi,
                                           @PathVariable("flagId") long flagId)
      throws IOException {
    ArticleRevisionIdentifier articleRevisionId = ArticleRevisionIdentifier.create(
        DoiEscaping.unescape(articleDoi), revisionNumber);
    CommentIdentifier commentId = CommentIdentifier.create(DoiEscaping.unescape(commentDoi));
    // TODO: Validate articleRevisionId and commentId

    commentCrudService.deleteCommentFlag(flagId);
    return reportOk(Long.toString(flagId));
  }

}
