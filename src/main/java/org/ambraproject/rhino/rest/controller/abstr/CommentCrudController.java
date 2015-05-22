package org.ambraproject.rhino.rest.controller.abstr;

import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.service.AnnotationCrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class CommentCrudController extends RestController {

  private static final String COMMENT_META_NAMESPACE = "/comments/";

  @Autowired
  private AnnotationCrudService annotationCrudService;

  @RequestMapping(value = COMMENT_META_NAMESPACE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam(ID_PARAM) String id)
      throws IOException {
    annotationCrudService.readComment(DoiBasedIdentity.create(id)).respond(request, response, entityGson);
  }

}
