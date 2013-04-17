/*
 * Copyright (c) 2006-2012 by Public Library of Science
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

import com.google.common.base.Optional;
import com.google.common.io.Closeables;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.controller.abstr.ArticleSpaceController;
import org.ambraproject.rhino.service.DoiBasedCrudService.WriteMode;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.util.response.ServletJsonpReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Controller for _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 */
@Controller
public class ArticleCrudController extends ArticleSpaceController {

  private static final Logger log = LoggerFactory.getLogger(ArticleCrudController.class);

  /**
   * The request parameter whose value is the XML file being uploaded for a create operation.
   */
  private static final String ARTICLE_XML_FIELD = "xml";


  @RequestMapping(value = ARTICLE_ROOT, method = RequestMethod.GET)
  public void listDois(HttpServletRequest request, HttpServletResponse response,
                       @RequestParam(value = METADATA_FORMAT_PARAM, required = false) String format)
      throws IOException {
    MetadataFormat mf = MetadataFormat.getFromParameter(format, true);
    ResponseReceiver receiver = ServletJsonpReceiver.create(request, response);
    articleCrudService.listDois(receiver, getParameters(request), mf);
  }

  /**
   * Create an article received at the root noun, without an identifier in the URL. Respond with the received data.
   *
   * @param response
   * @param requestFile
   * @throws IOException
   * @throws FileStoreException
   */
  @RequestMapping(value = ARTICLE_ROOT, method = RequestMethod.POST)
  public void create(HttpServletRequest request, HttpServletResponse response,
                     @RequestParam(ARTICLE_XML_FIELD) MultipartFile requestFile)
      throws IOException, FileStoreException {
    Article result;
    InputStream requestBody = null;
    boolean threw = true;
    try {
      requestBody = requestFile.getInputStream();
      result = articleCrudService.write(requestBody, Optional.<ArticleIdentity>absent(), WriteMode.CREATE_ONLY);
      threw = false;
    } finally {
      Closeables.close(requestBody, threw);
    }
    response.setStatus(HttpStatus.CREATED.value());

    // Report the written data, as JSON, in the response.
    ResponseReceiver receiver = ServletJsonpReceiver.create(request, response);
    articleCrudService.readMetadata(receiver, result, MetadataFormat.JSON);
  }

  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam(value = METADATA_FORMAT_PARAM, required = false) String format)
      throws FileStoreException, IOException {
    ArticleIdentity id = parse(request);
    MetadataFormat mf = MetadataFormat.getFromParameter(format, true);
    ResponseReceiver receiver = ServletJsonpReceiver.create(request, response);
    articleCrudService.readMetadata(receiver, id, mf);
  }

  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<?> delete(HttpServletRequest request) throws FileStoreException {
    ArticleIdentity id = parse(request);
    articleCrudService.delete(id);
    return reportOk();
  }

}
