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

package org.ambraproject.admin.controller;

import com.google.common.base.Optional;
import org.ambraproject.admin.service.ArticleCrudService;
import org.ambraproject.admin.service.DoiBasedCrudService.WriteMode;
import org.ambraproject.admin.service.DoiBasedCrudService.WriteResult;
import org.ambraproject.filestore.FileStoreException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

/**
 * Controller for _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 */
@Controller
public class ArticleCrudController extends DoiBasedCrudController {

  private static final Logger log = LoggerFactory.getLogger(ArticleCrudController.class);

  private static final String ARTICLE_NAMESPACE = "/article/";
  private static final String ARTICLE_TEMPLATE = ARTICLE_NAMESPACE + "**";

  @Autowired
  private ArticleCrudService articleCrudService;

  @Override
  protected String getNamespacePrefix() {
    return ARTICLE_NAMESPACE;
  }


  /**
   * Create an article received at the root noun, without an identifier in the URL.
   * <p/>
   * TODO: Handle the case where the article already exists
   *
   * @param requestBody
   * @return
   */
  @RequestMapping(value = ARTICLE_NAMESPACE, method = RequestMethod.PUT)
  public ResponseEntity<?> create(InputStream requestBody) throws IOException, FileStoreException {
    try {
      articleCrudService.write(requestBody, Optional.<DoiBasedIdentity>absent(), WriteMode.CREATE_ONLY);
    } finally {
      IOUtils.closeQuietly(requestBody);
    }
    return reportCreated();
  }

  /**
   * Dispatch an action to upload an article.
   *
   * @param request the HTTP request from a REST client
   * @return the HTTP response, to indicate success or describe an error
   * @throws IOException
   * @throws FileStoreException
   */
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.PUT)
  public ResponseEntity<?> upload(HttpServletRequest request)
      throws IOException, FileStoreException {
    DoiBasedIdentity id = parse(request);
    InputStream stream = null;
    WriteResult result;
    try {
      stream = request.getInputStream();
      result = articleCrudService.write(stream, Optional.of(id), WriteMode.WRITE_ANY);
    } finally {
      IOUtils.closeQuietly(stream);
    }
    return new ResponseEntity<Object>(result.getStatus());
  }

  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.GET)
  public ResponseEntity<?> read(HttpServletRequest request,
                                @RequestParam(value = METADATA_FORMAT_PARAM, required = false) String format)
      throws FileStoreException, IOException {
    DoiBasedIdentity id = parse(request);
    MetadataFormat mf = MetadataFormat.getFromParameter(format);
    String json = articleCrudService.readMetadata(id, mf);
    return new ResponseEntity<String>(json, HttpStatus.OK);
  }

  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<?> delete(HttpServletRequest request) throws FileStoreException {
    throw new RuntimeException("Not implemented yet"); // TODO
  }

}
