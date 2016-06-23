package org.ambraproject.rhino.rest.controller;

import com.wordnik.swagger.annotations.ApiOperation;
import org.ambraproject.rhino.identity.CommentIdentifier;
import org.ambraproject.rhino.model.Comment;
import org.ambraproject.rhino.model.Flag;
import org.ambraproject.rhino.rest.controller.abstr.DoiBasedCrudController;
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
public class CommentCrudController extends DoiBasedCrudController {

  protected static final String COMMENT_META_ROOT = "/comments";
  protected static final String COMMENT_META_NAMESPACE = COMMENT_META_ROOT + "/";
  protected static final String COMMENT_META_TEMPLATE = COMMENT_META_NAMESPACE + "/**";

  protected static final String FLAGS_META_ROOT = "/flags";
  protected static final String FLAGS_META_TEMPLATE = FLAGS_META_ROOT + "/{flagId}";

  @Autowired
  protected CommentCrudService commentCrudService;

  @Override
  protected final String getNamespacePrefix() {
    return COMMENT_META_NAMESPACE;
  }

  protected final CommentIdentifier parseCommentId(HttpServletRequest request) {
    return CommentIdentifier.create(getIdentifier(request));
  }

  private static final String FLAGS_PARAMETER = "flags";
  private static final String FLAGGED_PARAMETER = "flagged";

  @RequestMapping(value = COMMENT_META_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String commentUri = parseCommentId(request).getDoiName();
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
    CommentIdentifier commentId = parseCommentId(request);
    commentCrudService.removeFlagsFromComment(commentId);
    return reportOk(commentId.toString());
  }

  @RequestMapping(value = COMMENT_META_TEMPLATE, method = RequestMethod.PATCH)
  public ResponseEntity<?> patch(HttpServletRequest request)
      throws IOException {
    CommentIdentifier commentId = parseCommentId(request);
    CommentInputView input = readJsonFromRequest(request, CommentInputView.class);
    Comment patched = commentCrudService.patchComment(commentId, input);
    return reportOk(patched.getCommentUri());
  }

  @RequestMapping(value = COMMENT_META_TEMPLATE, method = RequestMethod.DELETE)
  @ApiOperation(value = "delete", notes = "Performs a hard delete operation in the database. " +
      "NOTE: fails loudly if attempting to delete a comment that has any replies. All replies must " +
      "be deleted first.")
  public ResponseEntity<?> delete(HttpServletRequest request)
      throws IOException {
    CommentIdentifier commentId = parseCommentId(request);
    String deletedCommentUri = commentCrudService.deleteComment(commentId);
    return reportOk(deletedCommentUri);
  }

  @RequestMapping(value = COMMENT_META_TEMPLATE, method = RequestMethod.POST, params = {FLAGS_PARAMETER})
  public ResponseEntity<String> createFlag(HttpServletRequest request) throws IOException {
    CommentIdentifier commentId = parseCommentId(request);
    CommentFlagInputView input = readJsonFromRequest(request, CommentFlagInputView.class);
    Flag commentFlag = commentCrudService.createCommentFlag(commentId, input);
    return reportCreated(commentFlag.getCommentFlagId().toString());
  }

  @RequestMapping(value = COMMENT_META_TEMPLATE, method = RequestMethod.GET, params = {FLAGS_PARAMETER})
  public void readFlagsOnComment(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    CommentIdentifier commentId = parseCommentId(request);
    commentCrudService.readCommentFlagsOn(commentId).respond(request, response, entityGson);
  }

  @RequestMapping(value = FLAGS_META_TEMPLATE, method = RequestMethod.GET)
  public void readFlag(HttpServletRequest request, HttpServletResponse response,
                       @PathVariable("flagId") Long flagId)
      throws IOException {
    commentCrudService.readCommentFlag(flagId).respond(request, response, entityGson);
  }

  @RequestMapping(value = FLAGS_META_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<Object> removeFlag(@PathVariable("flagId") Long flagId)
      throws IOException {
    commentCrudService.deleteCommentFlag(flagId);
    return reportOk(flagId.toString());
  }

}
