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
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.rest.DoiEscaping;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleStateService;
import org.ambraproject.rhino.view.article.ArticleInputView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Controller class used for reading and writing an article's state after its creation.
 */
@Controller
public class ArticleStateController extends RestController {

  private static final Logger log = LoggerFactory.getLogger(ArticleStateController.class);

  @Autowired
  private ArticleCrudService articleCrudService;
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
  @RequestMapping(value = "/versioned/articles/{doi}/revisions/{number}", method = RequestMethod.PATCH)
  public void write(HttpServletRequest request, HttpServletResponse response,
                    @PathVariable("doi") String doi,
                    @PathVariable("number") int revisionNumber)
      throws IOException {
    ArticleRevisionIdentifier revisionId = ArticleRevisionIdentifier.create(DoiEscaping.resolve(doi), revisionNumber);

    ArticleIdentity id = null; // TODO: Reimplement for ArticleRevision
    ArticleInputView input = readJsonFromRequest(request, ArticleInputView.class);
    articleStateService.update(id, input);
    articleCrudService.readMetadata(id, false).respond(request, response, entityGson);
  }
}
