package org.ambraproject.rhino.rest.controller.abstr;

import org.ambraproject.models.Annotation;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.service.AnnotationCrudService;
import org.ambraproject.rhino.view.CommentInputView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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

  @Override
  protected String getNamespacePrefix() {
    return COMMENT_META_NAMESPACE;
  }

  @Autowired
  private AnnotationCrudService annotationCrudService;

  @RequestMapping(value = COMMENT_META_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    DoiBasedIdentity id = parse(request);
    annotationCrudService.readComment(id).respond(request, response, entityGson);
  }

  @RequestMapping(value = COMMENT_META_ROOT, method = RequestMethod.POST)
  public ResponseEntity<?> create(HttpServletRequest request)
      throws IOException {
    CommentInputView input = readJsonFromRequest(request, CommentInputView.class);
    Annotation created = annotationCrudService.createComment(input);
    return new ResponseEntity<>(HttpStatus.CREATED); // TODO: Report (at minimum) created annotationUri
  }

}
