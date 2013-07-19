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
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.controller.abstr.DoiBasedCrudController;
import org.ambraproject.rhino.service.AnnotationCrudService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.util.response.ServletResponseReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controller that handles CRUD for article corrections and comments.
 * Although the API presents these entities as unrelated, article-level things,
 * they currently share a common backend implementation, which is why they're
 * in the same controller (and service).
 *
 * TODO: CUD
 */
@Controller
public class AnnotationCrudController extends DoiBasedCrudController {

  private static final String CORRECTION_ROOT = "/corrections";
  private static final String CORRECTION_NAMESPACE = CORRECTION_ROOT + '/';
  private static final String CORRECTION_TEMPLATE = CORRECTION_NAMESPACE + "**";

  private static final String COMMENT_ROOT = "/comments";
  private static final String COMMENT_NAMESPACE = COMMENT_ROOT + '/';
  private static final String COMMENT_TEMPLATE = COMMENT_NAMESPACE + "**";

  @Autowired
  private AnnotationCrudService annotationCrudService;

  // Bit of a hack: since this controller class handles two namespace prefixes,
  // we don't use getNamespacePrefix() and instead override getIdentifier().

  @Override
  protected final String getNamespacePrefix() {
    throw new IllegalStateException("Should never be called");
  }

  @Override
  protected String getIdentifier(HttpServletRequest request) {
    String namespacePrefix;
    if (request.getRequestURI().startsWith(CORRECTION_NAMESPACE)) {
      namespacePrefix = CORRECTION_NAMESPACE;
    } else if (request.getRequestURI().startsWith(COMMENT_NAMESPACE)) {
      namespacePrefix = COMMENT_NAMESPACE;
    } else {
      throw new IllegalArgumentException("Unknown namespace prefix: " + request.getRequestURI());
    }
    return getFullPathVariable(request, namespacePrefix);
  }

  @Override
  protected final ArticleIdentity parse(HttpServletRequest request) {
    return ArticleIdentity.create(getIdentifier(request));
  }

  /**
   * Handles GET/read requests for all the corrections associated with an article.
   * The response will be a list of objects representing corrections.  Each correction
   * has a "replies" list that contains any replies (recursively).
   *
   * @param request HttpServletRequest
   * @param response HttpServletResponse
   * @param format must be "json" currently
   * @throws FileStoreException
   * @throws IOException
   */
  @RequestMapping(value = CORRECTION_TEMPLATE, method = RequestMethod.GET)
  public void readCorrections(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam(value = METADATA_FORMAT_PARAM, required = false) String format)
      throws FileStoreException, IOException {
    ArticleIdentity id = parse(request);
    MetadataFormat mf = MetadataFormat.getFromParameter(format, true);
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(request, response);
    annotationCrudService.readCorrections(receiver, id, mf);
  }

  /**
   * Handles GET/read requests for all the comments associated with an article.
   * The response will be a list of objects representing comments.  Each comment
   * has a "replies" list that contains any replies (recursively).
   *
   * @param request HttpServletRequest
   * @param response HttpServletResponse
   * @param format must be "json" currently
   * @throws FileStoreException
   * @throws IOException
   */
  @RequestMapping(value = COMMENT_TEMPLATE, method = RequestMethod.GET)
  public void readComments(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam(value = METADATA_FORMAT_PARAM, required = false) String format)
      throws FileStoreException, IOException {
    ArticleIdentity id = parse(request);
    MetadataFormat mf = MetadataFormat.getFromParameter(format, true);
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(request, response);
    annotationCrudService.readComments(receiver, id, mf);
  }
}
