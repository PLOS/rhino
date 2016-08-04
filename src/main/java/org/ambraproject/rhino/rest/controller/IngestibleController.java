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
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.rest.DoiEscaping;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.IngestibleService;
import org.ambraproject.rhino.service.impl.VersionedIngestionService;
import org.ambraproject.rhino.util.Archive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

/**
 * Controller enabling access to the ambra ingest directory (whose location is defined by the
 * ambra.services.documentManagement.ingestSourceDir property of ambra.xml).
 */
@Controller
public class IngestibleController extends RestController {

  private static final String INGESTIBLE_ROOT = "/ingestibles";

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private IngestibleService ingestibleService;
  @Autowired
  private VersionedIngestionService versionedIngestionService;

  /**
   * Method that lists all ingestible archives in the ingest source directory.
   *
   * @param response HttpServletResponse
   * @throws IOException
   */
  // TODO: Remove method if possible
  @Transactional(readOnly = true)
  @RequestMapping(value = INGESTIBLE_ROOT, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    ingestibleService.read().respond(request, response, entityGson);
  }

  /**
   * Ingests an archive present in the ingest source directory.
   *
   * @param response      HttpServletResponse
   * @param name          the name of an ingestible archive present in the ingest source directory
   * @param forceReingest if present, we will reingest the article if it already exists
   * @throws IOException
   */
  // TODO: Remove method if possible
  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = INGESTIBLE_ROOT, method = RequestMethod.POST)
  public void ingest(HttpServletRequest request, HttpServletResponse response,
                     @RequestParam(value = "name") String name,
                     @RequestParam(value = "force_reingest", required = false) String forceReingest,
                     @RequestParam(value = "revision", required = false) Integer revisionNumber)
      throws IOException {

    File archiveFile;
    try {
      archiveFile = ingestibleService.getIngestibleArchive(name);
    } catch (FileNotFoundException fnfe) {
      throw new RestClientException("Could not find ingestible archive for: " + name,
          HttpStatus.METHOD_NOT_ALLOWED, fnfe);
    }

    // TODO: Add user-specific (i.e., PLOS-vs-non-PLOS) way to infer expected ID from zip file naming convention.
    Optional<ArticleIdentity> expectedId = Optional.empty();

    ArticleIngestionIdentifier ingestionId;
    try (Archive archive = Archive.readZipFile(archiveFile)) {
      ingestionId = versionedIngestionService.ingest(archive);
    }
    ingestibleService.archiveIngested(name);
    response.setStatus(HttpStatus.CREATED.value());

    // Report the written data, as JSON, in the response.
    articleCrudService.serveMetadata(ingestionId).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/articles/{doi}/ingestions/{number}/ingestible", method = RequestMethod.GET)
  public void repack(HttpServletResponse response,
                     @PathVariable("doi") String doi,
                     @PathVariable("number") int ingestionNumber)
      throws IOException {
    ArticleIngestionIdentifier ingestionId = ArticleIngestionIdentifier.create(DoiEscaping.unescape(doi), ingestionNumber);

    String articleId = null; // TODO: Implement ArticleCrudService.repack for ArticleIngestionIdentifier
    Archive archive = articleCrudService.repack(ArticleIdentity.create(articleId));
    response.setStatus(HttpStatus.OK.value());
    response.setContentType("application/zip");
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "filename=" + archive.getArchiveName());
    try (OutputStream outputStream = response.getOutputStream()) {
      archive.write(outputStream);
    }
  }

}
