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

import com.google.common.base.Optional;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.controller.abstr.DoiBasedCrudController;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.DoiBasedCrudService;
import org.ambraproject.rhino.service.DoiBasedCrudService.WriteMode;
import org.ambraproject.rhino.service.IngestableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
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

  /**
   * Ingests an archive present in the ingest source directory.
   *
   * @param response HttpServletResponse
   * @param doi a DOI corresponding to an article whose .zip archive is present
   *     in the ingest source directory
   * @param forceReingest if present, we will reingest the article if it already exists
   * @throws IOException
   * @throws FileStoreException
   */
  @RequestMapping(value = INGESTABLE_ROOT, method = RequestMethod.POST)
  public void ingest(HttpServletResponse response, @RequestParam(value = "doi") String doi,
      @RequestParam(value = "force_reingest", required = false) String forceReingest)
      throws IOException, FileStoreException {

    ArticleIdentity ai = ArticleIdentity.create(doi);
    File archive = ingestableService.getIngestableArchive(ai);
    Article result = articleCrudService.writeArchive(archive.getCanonicalPath(), Optional.of(ai),

        // If forceReingest is the empty string, the parameter was present.  Only
        // treat null as false.
        forceReingest == null ? WriteMode.CREATE_ONLY : WriteMode.WRITE_ANY);
    ingestableService.archiveIngested(ai);
    response.setStatus(HttpStatus.CREATED.value());

    // Report the written data, as JSON, in the response.
    articleCrudService.readMetadata(response, result, MetadataFormat.JSON);
  }
}
