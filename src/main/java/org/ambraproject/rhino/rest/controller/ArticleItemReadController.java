package org.ambraproject.rhino.rest.controller;

import com.google.gson.Gson;
import org.ambraproject.rhino.identity.ArticleItemIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.rest.controller.abstr.DoiBasedCrudController;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.ArticleItemView;
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

@Controller
public class ArticleItemReadController extends DoiBasedCrudController {

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
    Doi id = Doi.create(parse(request).getIdentifier());
    int revisionNumberValue = (revisionNumber == null) ? articleCrudService.getLatestRevision(id) : revisionNumber;
    ArticleItem work = articleCrudService.getArticleItem(ArticleItemIdentifier.create(id, revisionNumberValue));
    asTransceiver(work).respond(request, response, entityGson);
  }

  // TODO: Extract to service class
  private Transceiver asTransceiver(ArticleItem work) {
    return new Transceiver() {
      @Override
      protected Object getData() throws IOException {
        return new ArticleItemView(work);
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

}
