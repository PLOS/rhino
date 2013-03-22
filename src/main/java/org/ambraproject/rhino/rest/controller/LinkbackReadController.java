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

import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.controller.abstr.DoiBasedCrudController;
import org.ambraproject.rhino.service.LinkbackReadService;
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

@Controller
public class LinkbackReadController extends DoiBasedCrudController {

  private static final String LINKBACK_ROOT = "/linkback";
  private static final String LINKBACK_NAMESPACE = LINKBACK_ROOT + '/';
  private static final String LINKBACK_TEMPLATE = LINKBACK_NAMESPACE + "**";

  private static final String LINKBACK_TYPE_PARAM = "type";

  @Override
  protected String getNamespacePrefix() {
    return LINKBACK_NAMESPACE;
  }

  @Autowired
  private LinkbackReadService linkbackReadService;

  @RequestMapping(value = LINKBACK_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam(value = LINKBACK_TYPE_PARAM, required = false) String linkbackType)
      throws IOException {
    ArticleIdentity id = ArticleIdentity.create(getIdentifier(request));
    ResponseReceiver receiver = ServletJsonpReceiver.create(request, response);
    linkbackReadService.read(receiver, id, MetadataFormat.JSON);
  }

}
