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

import com.google.common.base.Strings;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.ClassificationService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.util.response.ServletResponseReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;

/**
 * Controller class for the taxonomy namespace.
 */
@Controller
public class TaxonomyController extends RestController {

  private static final String TAXONOMY_ROOT = "/taxonomy";
  private static final String TAXONOMY_NAMESPACE = TAXONOMY_ROOT + '/';
  private static final String TAXONOMY_TEMPLATE = TAXONOMY_NAMESPACE + "**";

  @Autowired
  private ClassificationService classificationService;

  @RequestMapping(value = TAXONOMY_TEMPLATE, method = RequestMethod.GET)
  public void readRoot(HttpServletRequest request, HttpServletResponse response,
                       @RequestParam(value = "journal", required = true) String journal,
                       @RequestParam(value = JSONP_CALLBACK_PARAM, required = false) String jsonp,
                       @RequestHeader(value = ACCEPT_REQUEST_HEADER, required = false) String accept) throws Exception {
    String parent = getFullPathVariable(request, true, TAXONOMY_NAMESPACE);
    if (!Strings.isNullOrEmpty(parent)) {
      parent = URLDecoder.decode(parent, "UTF-8");
    }
    MetadataFormat metadataFormat = MetadataFormat.getFromAcceptHeader(accept);
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(jsonp, response);
    classificationService.read(journal, parent, receiver, metadataFormat);
  }
}
