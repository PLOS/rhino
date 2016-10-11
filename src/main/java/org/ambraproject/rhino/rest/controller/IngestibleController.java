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

import com.google.common.net.HttpHeaders;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.rest.DoiEscaping;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.util.Archive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Controller enabling access to the ambra ingest directory (whose location is defined by the
 * ambra.services.documentManagement.ingestSourceDir property of ambra.xml).
 */
@Controller
public class IngestibleController extends RestController {

  @Autowired
  private ArticleCrudService articleCrudService;

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/articles/{doi}/ingestions/{number}/ingestible", method = RequestMethod.GET)
  public void repack(HttpServletResponse response,
                     @PathVariable("doi") String doi,
                     @PathVariable("number") int ingestionNumber)
      throws IOException {
    ArticleIngestionIdentifier ingestionId = ArticleIngestionIdentifier.create(DoiEscaping.unescape(doi), ingestionNumber);

    Archive archive = articleCrudService.repack(ingestionId);
    response.setStatus(HttpStatus.OK.value());
    response.setContentType("application/zip");
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "filename=" + archive.getArchiveName());
    try (OutputStream outputStream = response.getOutputStream()) {
      archive.write(outputStream);
    }
  }

}
