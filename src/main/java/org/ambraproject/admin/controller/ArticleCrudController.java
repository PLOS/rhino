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

import org.ambraproject.admin.service.AmbraService;
import org.ambraproject.admin.service.ArticleCrudService;
import org.ambraproject.admin.service.DoiBasedCrudService;
import org.ambraproject.filestore.FileStoreException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

/**
 * Controller for _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 */
@Controller
public class ArticleCrudController extends FileStoreController {

  private static final Logger log = LoggerFactory.getLogger(ArticleCrudController.class);

  private static final String ARTICLE_NAMESPACE = "/article/";
  private static final String ARTICLE_TEMPLATE = ARTICLE_NAMESPACE + "**";

  @Autowired
  private ArticleCrudService articleCrudService;

  @Override
  protected DoiBasedCrudService getService() {
    return articleCrudService;
  }

  @Override
  protected String getNamespacePrefix() {
    return ARTICLE_NAMESPACE;
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
    AmbraService.UploadResult result;
    try {
      stream = request.getInputStream();
      result = articleCrudService.upload(stream, id);
    } finally {
      IOUtils.closeQuietly(stream);
    }
    return new ResponseEntity<Object>(result.getStatus());
  }


  @Override
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.GET)
  public ResponseEntity<?> read(HttpServletRequest request) throws FileStoreException, IOException {
    return super.read(request);
  }

  @Override
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<?> delete(HttpServletRequest request) throws FileStoreException {
    return super.delete(request);
  }

}
