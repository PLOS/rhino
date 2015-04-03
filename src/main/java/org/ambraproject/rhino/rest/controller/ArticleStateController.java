/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2013 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.controller.abstr.ArticleSpaceController;
import org.ambraproject.rhino.service.ArticleStateService;
import org.ambraproject.rhino.view.article.ArticleInputView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Controller class used for reading and writing an article's state after its creation.
 */
@Controller
public class ArticleStateController extends ArticleSpaceController {

  private static final Logger log = LoggerFactory.getLogger(ArticleStateController.class);

  @Autowired
  private ArticleStateService articleStateService;


  /**
   * Sets the state of an article based on JSON in the request.
   *
   * @param request  HttpServletRequest
   * @param response HttpServletResponse
   * @throws IOException
   */
  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = ARTICLE_ROOT, method = RequestMethod.PATCH)
  public void write(HttpServletRequest request, HttpServletResponse response,
                    @RequestParam(ID_PARAM) String id)
      throws IOException {
    ArticleIdentity articleIdentity = ArticleIdentity.create(id);
    ArticleInputView input = readJsonFromRequest(request, ArticleInputView.class);
    articleStateService.update(articleIdentity, input);
    articleCrudService.readMetadata(articleIdentity, false).respond(request, response, entityGson);
  }
}
