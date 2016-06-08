package org.ambraproject.rhino.rest.controller;

import com.google.gson.Gson;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.rest.controller.abstr.DoiBasedCrudController;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.ScholarlyWorkView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Calendar;
import java.util.OptionalInt;

@Controller
public class ScholarlyWorkReadController extends DoiBasedCrudController {

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private Gson entityGson;

  @Override
  protected String getNamespacePrefix() {
    return "/work/";
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/work/**", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam(value = "revision", required = false) Integer revisionNumber)
      throws IOException {
    DoiBasedIdentity id = parse(request);
    ArticleItem work = articleCrudService.getArticleItem(id,
        (revisionNumber == null) ? OptionalInt.empty() : OptionalInt.of(revisionNumber));
    asTransceiver(work).respond(request, response, entityGson);
  }

  // TODO: Extract to service class
  private Transceiver asTransceiver(ArticleItem work) {
    return new Transceiver() {
      @Override
      protected Object getData() throws IOException {
        return new ScholarlyWorkView(work);
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

}
