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

import org.ambraproject.admin.service.ArticleCrudService;
import org.ambraproject.admin.service.AssetCrudService;
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
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

/**
 * Controller for _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 */
@Controller
public class ArticleCrudController extends ArticleController {

  private static final Logger log = LoggerFactory.getLogger(ArticleCrudController.class);

  private static final String FILE_ARG = "file";
  private static final String ASSET_PARAM = "assetOf";

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private AssetCrudService assetCrudService;


  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.POST)
  public ResponseEntity<?> create(HttpServletRequest request, @RequestParam(FILE_ARG) MultipartFile file)
      throws IOException, FileStoreException {
    String doi = parseEntityDoi(request);
    String assetOf = request.getParameter(ASSET_PARAM);

    InputStream stream = null;
    try {
      stream = file.getInputStream();
      if (assetOf == null) {
        articleCrudService.create(stream, doi);
      } else {
        assetCrudService.create(stream, doi, assetOf);
      }
    } finally {
      IOUtils.closeQuietly(stream);
    }
    return reportOk();
  }

  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.GET)
  public ResponseEntity<?> read(HttpServletRequest request) throws FileStoreException {
    String doi = parseEntityDoi(request);
    byte[] fileData = articleCrudService.read(doi);
    return new ResponseEntity<byte[]>(fileData, HttpStatus.OK);
  }

  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.PUT)
  public ResponseEntity<?> update(HttpServletRequest request, @RequestParam("file") MultipartFile file)
      throws IOException, FileStoreException {
    String doi = parseEntityDoi(request);
    InputStream stream = null;
    try {
      stream = file.getInputStream();
      articleCrudService.update(stream, doi);
    } finally {
      IOUtils.closeQuietly(stream);
    }
    return reportOk();
  }

  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<?> delete(HttpServletRequest request) throws FileStoreException {
    String doi = parseEntityDoi(request);
    articleCrudService.delete(doi);
    return reportOk();
  }

}
