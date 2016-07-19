package org.ambraproject.rhino.rest.controller;

import com.google.gson.Gson;
import org.ambraproject.rhino.identity.ArticleItemIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.rest.ClientItemId;
import org.ambraproject.rhino.rest.DoiEscaping;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.ArticleItemView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Calendar;

@Controller
public class ArticleItemReadController extends RestController {

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private Gson entityGson;

  @Transactional(readOnly = true)
  @RequestMapping(value = "/works/{doi:.+}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("doi") String doi)
      throws IOException {
    Doi doiObj = DoiEscaping.resolve(doi);

    // TODO: What do we want to do here? Given a DOI, resolve to an object type? Do ingestion/revision number matter?
    ClientItemId id = null;
    ArticleItemIdentifier itemId = articleCrudService.resolveToItem(id);
    ArticleItem work = articleCrudService.getArticleItem(itemId);
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
