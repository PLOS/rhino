/*
 * Copyright (c) 2006-2013 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.rest.controller;

import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.controller.abstr.DoiBasedCrudController;
import org.ambraproject.rhino.service.PingbackReadService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.util.response.ServletJsonpReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.ambraproject.rhino.rest.controller.ArticleCrudController.ARTICLE_NAMESPACE;
import static org.ambraproject.rhino.rest.controller.ArticleCrudController.ARTICLE_ROOT;
import static org.ambraproject.rhino.rest.controller.ArticleCrudController.ARTICLE_TEMPLATE;


@Controller
public class PingbackReadController extends DoiBasedCrudController {

  private static final String PINGBACK_PARAM = "pingbacks";

  @Override
  protected String getNamespacePrefix() {
    return ARTICLE_NAMESPACE;
  }

  @Override
  protected ArticleIdentity parse(HttpServletRequest request) {
    return ArticleIdentity.create(getIdentifier(request));
  }


  @Autowired
  private PingbackReadService pingbackReadService;

  @RequestMapping(value = ARTICLE_ROOT, method = RequestMethod.GET, params = {PINGBACK_PARAM})
  public void listPingbacks(HttpServletRequest request, HttpServletResponse response,
                            @RequestParam(value = METADATA_FORMAT_PARAM, required = false) String format)
      throws IOException {
    MetadataFormat mf = MetadataFormat.getFromParameter(format, true);
    ResponseReceiver receiver = ServletJsonpReceiver.create(request, response);
    pingbackReadService.listByArticle(receiver, mf, PingbackReadService.OrderBy.COUNT);
  }

  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.GET, params = {PINGBACK_PARAM})
  public void readPingbacks(HttpServletRequest request, HttpServletResponse response,
                            @RequestParam(value = METADATA_FORMAT_PARAM, required = false) String format)
      throws FileStoreException, IOException {
    ArticleIdentity id = parse(request);
    MetadataFormat mf = MetadataFormat.getFromParameter(format, true);
    ResponseReceiver receiver = ServletJsonpReceiver.create(request, response);
    pingbackReadService.read(receiver, id, mf);
  }


}
