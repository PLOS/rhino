package org.ambraproject.rhino.rest.controller.abstr;

import org.ambraproject.models.Annotation;
import org.ambraproject.models.Flag;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.service.AnnotationCrudService;
import org.ambraproject.rhino.view.comment.CommentFlagInputView;
import org.ambraproject.rhino.view.comment.CommentInputView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class CommentCrudController extends DoiBasedCrudController {

  private static final String COMMENT_META_ROOT = "/comments";
  private static final String FLAGS_META_ROOT = COMMENT_META_ROOT + "/flags";
  private static final String FLAGS_META_TEMPLATE = FLAGS_META_ROOT+ "/**";
  private static final String COMMENT_META_FLAGGED_ROOT = "/flagged";
  private static final String COMMENT_META_NAMESPACE = COMMENT_META_ROOT + "/";
  private static final String COMMENT_META_TEMPLATE = COMMENT_META_NAMESPACE + "/**";
  private static final String COMMENT_META_FLAGGED_TEMPLATE = COMMENT_META_NAMESPACE + COMMENT_META_FLAGGED_ROOT;

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

  @RequestMapping(value = COMMENT_META_FLAGGED_TEMPLATE, method = RequestMethod.GET)
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

  @RequestMapping(value = COMMENT_META_TEMPLATE, method = RequestMethod.DELETE,
      params = "removeAllFlags")
  public ResponseEntity<?> removeAllFlags(HttpServletRequest request)
      throws IOException {
    DoiBasedIdentity commentId = parse(request);
    String commentUri = annotationCrudService.removeFlagsFromComment(commentId);
    return reportOk(commentUri);
  }

  @RequestMapping(value = COMMENT_META_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<?> remove(HttpServletRequest request)
      throws IOException {
    DoiBasedIdentity commentId = parse(request);
    String deletedCommentUri = annotationCrudService.removeComment(commentId);
    return reportOk(deletedCommentUri);
  }

  @RequestMapping(value = FLAGS_META_TEMPLATE, method = RequestMethod.POST)
  public ResponseEntity<String> createFlag(HttpServletRequest request) throws IOException {
    DoiBasedIdentity commentId = parse(request);
    CommentFlagInputView input = readJsonFromRequest(request, CommentFlagInputView.class);
    Flag commentFlag = annotationCrudService.createCommentFlag(commentId, input);
    return reportCreated(commentFlag.getID().toString());
  }

  @RequestMapping(value = FLAGS_META_TEMPLATE, method = RequestMethod.GET)
  public void readFlag(HttpServletRequest request, HttpServletResponse response,
      @RequestParam String flagId) throws IOException {
    annotationCrudService.readCommentFlag(flagId).respond(request, response, entityGson);
  }

  @RequestMapping(value = FLAGS_META_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<String> removeFlag(HttpServletRequest request, @RequestParam String flagId)
      throws IOException {
    String commentUri = annotationCrudService.deleteCommentFlag(flagId);
    return reportCreated(commentUri);
  }

}
