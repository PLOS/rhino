package org.ambraproject.rhino.rest.controller.abstr;

import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.service.AnnotationCrudService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.util.response.ServletResponseReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class CommentCrudController extends DoiBasedCrudController {

  private static final String COMMENT_META_NAMESPACE = "/comments/";
  private static final String COMMENT_META_TEMPLATE = COMMENT_META_NAMESPACE + "**";

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
    MetadataFormat mf = MetadataFormat.getFromRequest(request);
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(request, response);
    annotationCrudService.readComment(receiver, id, mf);
  }

}
