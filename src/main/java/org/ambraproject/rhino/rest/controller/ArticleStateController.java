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

import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.rhino.content.ArticleState;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.controller.abstr.DoiBasedCrudController;
import org.ambraproject.rhino.service.ArticleStateService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.util.response.ServletJsonpReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.ambraproject.rhino.rest.controller.ArticleCrudController.ARTICLE_NAMESPACE;
import static org.ambraproject.rhino.rest.controller.ArticleCrudController.ARTICLE_TEMPLATE;


/**
 * Controller class used for reading and writing an article's state after its creation.
 */
@Controller
public class ArticleStateController extends DoiBasedCrudController {

  private static final Logger log = LoggerFactory.getLogger(ArticleStateController.class);

  private static final String ARTICLE_STATE_PARAM = "state";

  @Autowired
  private ArticleStateService articleStateService;

  @Override
  protected String getNamespacePrefix() {
    return ARTICLE_NAMESPACE;
  }

  @Override
  protected ArticleIdentity parse(HttpServletRequest request) {
    return ArticleIdentity.create(getIdentifier(request));
  }

  /**
   * Reads the state of an article.
   *
   * @param request  HttpServletRequest
   * @param response HttpServletResponse
   * @param format
   * @throws IOException
   */
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.GET, params = {ARTICLE_STATE_PARAM})
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam(value = METADATA_FORMAT_PARAM, required = false) String format)
      throws IOException {

    ArticleIdentity id = parse(request);
    ResponseReceiver receiver = ServletJsonpReceiver.create(request, response);
    articleStateService.read(receiver, id, MetadataFormat.getFromParameter(format, true));
  }

  /**
   * Sets the state of an article based on JSON in the request.
   *
   * @param request  HttpServletRequest
   * @param response HttpServletResponse
   * @throws IOException
   */
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.PUT, params = {ARTICLE_STATE_PARAM})
  public void write(HttpServletRequest request, HttpServletResponse response)
      throws IOException, FileStoreException {

    ArticleIdentity id = parse(request);
    ArticleState state = readJsonFromRequest(request, ArticleState.class);
    articleStateService.write(id, state);
    ResponseReceiver receiver = ServletJsonpReceiver.create(request, response);
    articleStateService.read(receiver, id, MetadataFormat.JSON);
  }
}
