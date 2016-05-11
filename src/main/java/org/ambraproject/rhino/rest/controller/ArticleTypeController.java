package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.ArticleTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class ArticleTypeController extends RestController {

  @Autowired
  ArticleTypeService articleTypeService;

  /**
   * Retrieves a list of ArticleType objects in display order.
   *
   * @param request
   * @param response
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articleTypes", method = RequestMethod.GET)
  public void readArticleTypes(HttpServletRequest request, HttpServletResponse response)
          throws IOException {
    articleTypeService.listArticleTypes().respond(request, response, entityGson);
  }
}
