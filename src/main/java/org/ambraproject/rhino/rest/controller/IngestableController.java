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
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.controller.abstr.DoiBasedCrudController;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.IngestableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controller enabling access to the ambra ingest directory (whose location is defined
 * by the ambra.services.documentManagement.ingestSourceDir property of ambra.xml).
 */
@Controller
public class IngestableController extends DoiBasedCrudController {

  private static final String INGESTABLE_ROOT = "/ingestable";
  private static final String INGESTABLE_NAMESPACE = INGESTABLE_ROOT + "/";
  private static final String INGESTABLE_TEMPLATE = INGESTABLE_NAMESPACE + "**";

  @Autowired
  private ArticleCrudService articleCrudService;

  @Autowired
  private IngestableService ingestableService;

  @Override
  protected String getNamespacePrefix() {
    return INGESTABLE_NAMESPACE;
  }

  @Override
  protected ArticleIdentity parse(HttpServletRequest request) {
    return ArticleIdentity.create(getIdentifier(request));
  }

  /**
   * Method that lists all ingestable archives in the ingest source directory.
   *
   * @param response HttpServletResponse
   * @param format format of the response.  Currently only JSON is supported.
   * @throws IOException
   */
  @RequestMapping(value = INGESTABLE_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletResponse response,
      @RequestParam(value = METADATA_FORMAT_PARAM, required = false) String format)
      throws IOException {

    MetadataFormat mf = MetadataFormat.getFromParameter(format, true);
    ingestableService.read(response, mf);
  }
}
