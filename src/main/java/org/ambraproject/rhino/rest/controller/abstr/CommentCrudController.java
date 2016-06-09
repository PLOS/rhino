package org.ambraproject.rhino.rest.controller.abstr;

import com.wordnik.swagger.annotations.ApiOperation;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.model.Annotation;
import org.ambraproject.rhino.model.Flag;
import org.ambraproject.rhino.service.AnnotationCrudService;
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

  private static final String COMMENT_META_ROOT = "/comments";
  private static final String COMMENT_META_NAMESPACE = COMMENT_META_ROOT + "/";
  private static final String COMMENT_META_TEMPLATE = COMMENT_META_NAMESPACE + "/**";

  private static final String FLAGS_META_ROOT = "/flags";
  private static final String FLAGS_META_TEMPLATE = FLAGS_META_ROOT + "/{flagId}";
  private static final String FLAGS_PARAMETER = "flags";
  private static final String FLAGGED_PARAMETER = "flagged";

  @Override
  protected String getNamespacePrefix() {
    return COMMENT_META_NAMESPACE;
  }

  @Autowired
  private AnnotationCrudService annotationCrudService;

  @RequestMapping(value = COMMENT_META_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    DoiBasedIdentity commentId = parse(request);
    annotationCrudService.readComment(commentId).respond(request, response, entityGson);
  }

  @RequestMapping(value = COMMENT_META_ROOT, method = RequestMethod.GET, params = {FLAGGED_PARAMETER})
  public void readAllFlaggedComments(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    annotationCrudService.readFlaggedComments().respond(request, response, entityGson);
  }

  @RequestMapping(value = FLAGS_META_ROOT, method = RequestMethod.GET)
  public void readAllFlags(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    annotationCrudService.readAllCommentFlags().respond(request, response, entityGson);
  }

  @RequestMapping(value = COMMENT_META_ROOT, method = RequestMethod.POST)
  public ResponseEntity<?> create(HttpServletRequest request)
      throws IOException {
    CommentInputView input = readJsonFromRequest(request, CommentInputView.class);
    Annotation created = annotationCrudService.createComment(input);
    return reportCreated(created.getAnnotationUri());
  }

  @RequestMapping(value = COMMENT_META_TEMPLATE, method = RequestMethod.DELETE, params = FLAGS_PARAMETER)
  public ResponseEntity<?> removeAllFlags(HttpServletRequest request)
      throws IOException {
    DoiBasedIdentity commentId = parse(request);
    String commentUri = annotationCrudService.removeFlagsFromComment(commentId);
    return reportOk(commentUri);
  }

  @RequestMapping(value = COMMENT_META_TEMPLATE, method = RequestMethod.PATCH)
  public ResponseEntity<?> patch(HttpServletRequest request)
      throws IOException {
    DoiBasedIdentity commentId = parse(request);
    CommentInputView input = readJsonFromRequest(request, CommentInputView.class);
    Annotation patched = annotationCrudService.patchComment(commentId, input);
    return reportOk(patched.getAnnotationUri());
  }

  @RequestMapping(value = COMMENT_META_TEMPLATE, method = RequestMethod.DELETE)
  @ApiOperation(value = "delete", notes = "Performs a hard delete operation in the database. " +
      "NOTE: fails loudly if attempting to delete a comment that has any replies. All replies must " +
      "be deleted first.")
  public ResponseEntity<?> delete(HttpServletRequest request)
      throws IOException {
    DoiBasedIdentity commentId = parse(request);
    String deletedCommentUri = annotationCrudService.deleteComment(commentId);
    return reportOk(deletedCommentUri);
  }

  @RequestMapping(value = COMMENT_META_TEMPLATE, method = RequestMethod.POST, params = {FLAGS_PARAMETER})
  public ResponseEntity<String> createFlag(HttpServletRequest request) throws IOException {
    DoiBasedIdentity commentId = parse(request);
    CommentFlagInputView input = readJsonFromRequest(request, CommentFlagInputView.class);
    Flag commentFlag = annotationCrudService.createCommentFlag(commentId, input);
    return reportCreated(commentFlag.getID().toString());
  }

  @RequestMapping(value = COMMENT_META_TEMPLATE, method = RequestMethod.GET, params = {FLAGS_PARAMETER})
  public void readFlagsOnComment(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    DoiBasedIdentity commentId = parse(request);
    annotationCrudService.readCommentFlagsOn(commentId).respond(request, response, entityGson);
  }

  @RequestMapping(value = FLAGS_META_TEMPLATE, method = RequestMethod.GET)
  public void readFlag(HttpServletRequest request, HttpServletResponse response,
                       @PathVariable("flagId") String flagId)
      throws IOException {
    annotationCrudService.readCommentFlag(flagId).respond(request, response, entityGson);
  }

  @RequestMapping(value = FLAGS_META_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<Object> removeFlag(@PathVariable("flagId") String flagId)
      throws IOException {
    annotationCrudService.deleteCommentFlag(flagId);
    return reportOk(flagId);
  }

}
