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
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.controller.abstr.DoiBasedCrudController;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.DoiBasedCrudService.WriteMode;
import org.ambraproject.rhino.service.IngestibleService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.util.response.ServletResponseReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Controller enabling access to the ambra ingest directory (whose location is defined by the
 * ambra.services.documentManagement.ingestSourceDir property of ambra.xml).
 */
@Controller
public class IngestibleController extends DoiBasedCrudController {

  private static final String INGESTIBLE_ROOT = "/ingestibles";
  private static final String INGESTIBLE_NAMESPACE = INGESTIBLE_ROOT + "/";
  private static final String INGESTIBLE_TEMPLATE = INGESTIBLE_NAMESPACE + "**";

  @Autowired
  private ArticleCrudService articleCrudService;

  @Autowired
  private IngestibleService ingestibleService;

  @Override
  protected String getNamespacePrefix() {
    return INGESTIBLE_NAMESPACE;
  }

  @Override
  protected ArticleIdentity parse(HttpServletRequest request) {
    return ArticleIdentity.create(getIdentifier(request));
  }

  /**
   * Method that lists all ingestible archives in the ingest source directory.
   *
   * @param response HttpServletResponse
   * @param format   format of the response.  Currently only JSON is supported.
   * @throws IOException
   */
  @RequestMapping(value = INGESTIBLE_ROOT, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam(value = METADATA_FORMAT_PARAM, required = false) String format)
      throws IOException {

    MetadataFormat mf = MetadataFormat.getFromParameter(format, true);
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(request, response);
    ingestibleService.read(receiver, mf);
  }

  /**
   * Ingests an archive present in the ingest source directory.
   *
   * @param response      HttpServletResponse
   * @param name          the name of an ingestible archive present in the ingest source directory
   * @param forceReingest if present, we will reingest the article if it already exists
   * @throws IOException
   * @throws FileStoreException
   */
  @RequestMapping(value = INGESTIBLE_ROOT, method = RequestMethod.POST)
  public void ingest(HttpServletRequest request, HttpServletResponse response,
                     @RequestParam(value = "name") String name,
                     @RequestParam(value = "force_reingest", required = false) String forceReingest)
      throws IOException, FileStoreException {

    File archive;
    try {
      archive = ingestibleService.getIngestibleArchive(name);
    } catch (FileNotFoundException fnfe) {
      throw new RestClientException("Could not find ingestible archive for: " + name,
          HttpStatus.METHOD_NOT_ALLOWED, fnfe);
    }

    WriteMode reingestMode = booleanParameter(forceReingest) ? WriteMode.CREATE_ONLY : WriteMode.WRITE_ANY;

    // TODO: Add user-specific (i.e., PLOS-vs-non-PLOS) way to infer expected ID from zip file naming convention.
    Optional<ArticleIdentity> expectedId = Optional.absent();

    Article result = articleCrudService.writeArchive(archive.getCanonicalPath(), expectedId, reingestMode);
    ingestibleService.archiveIngested(name);
    response.setStatus(HttpStatus.CREATED.value());

    // Report the written data, as JSON, in the response.
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(request, response);
    articleCrudService.readMetadata(receiver, result, MetadataFormat.JSON);
  }
}
