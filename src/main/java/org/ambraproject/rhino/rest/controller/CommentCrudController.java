package org.ambraproject.rhino.rest.controller;

import com.wordnik.swagger.annotations.ApiOperation;
import org.ambraproject.rhino.model.Comment;
import org.ambraproject.rhino.model.Flag;
import org.ambraproject.rhino.rest.controller.abstr.CommentSpaceController;
import org.ambraproject.rhino.view.comment.CommentFlagInputView;
import org.ambraproject.rhino.view.comment.CommentInputView;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class CommentCrudController extends CommentSpaceController {

  private static final String FLAGS_PARAMETER = "flags";
  private static final String FLAGGED_PARAMETER = "flagged";

  @RequestMapping(value = COMMENT_META_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String commentUri = parse(request).getIdentifier();
    commentCrudService.readComment(commentUri).respond(request, response, entityGson);
  }

  @RequestMapping(value = COMMENT_META_ROOT, method = RequestMethod.GET, params = {FLAGGED_PARAMETER})
  public void readAllFlaggedComments(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    commentCrudService.readFlaggedComments().respond(request, response, entityGson);
  }

  @RequestMapping(value = FLAGS_META_ROOT, method = RequestMethod.GET)
  public void readAllFlags(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    commentCrudService.readAllCommentFlags().respond(request, response, entityGson);
  }

  @RequestMapping(value = COMMENT_META_ROOT, method = RequestMethod.POST)
  public ResponseEntity<?> create(HttpServletRequest request)
      throws IOException {
    CommentInputView input = readJsonFromRequest(request, CommentInputView.class);
    Comment created = commentCrudService.createComment(input);
    return reportCreated(created.getCommentUri());
  }

  @RequestMapping(value = COMMENT_META_TEMPLATE, method = RequestMethod.DELETE, params = FLAGS_PARAMETER)
  public ResponseEntity<?> removeAllFlags(HttpServletRequest request)
      throws IOException {
    String commentUri = parse(request).getIdentifier();
    commentCrudService.removeFlagsFromComment(commentUri);
    return reportOk(commentUri);
  }

  @RequestMapping(value = COMMENT_META_TEMPLATE, method = RequestMethod.PATCH)
  public ResponseEntity<?> patch(HttpServletRequest request)
      throws IOException {
    String commentUri = parse(request).getIdentifier();
    CommentInputView input = readJsonFromRequest(request, CommentInputView.class);
    Comment patched = commentCrudService.patchComment(commentUri, input);
    return reportOk(patched.getCommentUri());
  }

  @RequestMapping(value = COMMENT_META_TEMPLATE, method = RequestMethod.DELETE)
  @ApiOperation(value = "delete", notes = "Performs a hard delete operation in the database. " +
      "NOTE: fails loudly if attempting to delete a comment that has any replies. All replies must " +
      "be deleted first.")
  public ResponseEntity<?> delete(HttpServletRequest request)
      throws IOException {
    String commentUri = parse(request).getIdentifier();
    String deletedCommentUri = commentCrudService.deleteComment(commentUri);
    return reportOk(deletedCommentUri);
  }

  @RequestMapping(value = COMMENT_META_TEMPLATE, method = RequestMethod.POST, params = {FLAGS_PARAMETER})
  public ResponseEntity<String> createFlag(HttpServletRequest request) throws IOException {
    String commentUri = parse(request).getIdentifier();
    CommentFlagInputView input = readJsonFromRequest(request, CommentFlagInputView.class);
    Flag commentFlag = commentCrudService.createCommentFlag(commentUri, input);
    return reportCreated(commentFlag.getID().toString());
  }

  @RequestMapping(value = COMMENT_META_TEMPLATE, method = RequestMethod.GET, params = {FLAGS_PARAMETER})
  public void readFlagsOnComment(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String commentUri = parse(request).getIdentifier();
    commentCrudService.readCommentFlagsOn(commentUri).respond(request, response, entityGson);
  }

  @RequestMapping(value = FLAGS_META_TEMPLATE, method = RequestMethod.GET)
  public void readFlag(HttpServletRequest request, HttpServletResponse response,
                       @PathVariable("flagId") String flagId)
      throws IOException {
    commentCrudService.readCommentFlag(flagId).respond(request, response, entityGson);
  }

  @RequestMapping(value = FLAGS_META_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<Object> removeFlag(@PathVariable("flagId") String flagId)
      throws IOException {
    commentCrudService.deleteCommentFlag(flagId);
    return reportOk(flagId);
  }

}
