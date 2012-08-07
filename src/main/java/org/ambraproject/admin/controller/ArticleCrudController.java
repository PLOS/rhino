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
import org.ambraproject.filestore.FileStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Controller for _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 */
@Controller
public class ArticleCrudController extends RestController {

  private static final Logger log = LoggerFactory.getLogger(ArticleCrudController.class);

  private static final String DOI_SCHEME_VALUE = "info:doi/";

  private static final String DOI_PREFIX = "doiPrefix";
  private static final String DOI_ID = "doiId";
  private static final String DOI_TEMPLATE = "article/{" + DOI_PREFIX + "}/{" + DOI_ID + "}";
  private static final String FILE_ARG = "file";

  private static String buildDoi(String prefix, String id) {
    return DOI_SCHEME_VALUE + prefix + '/' + id;
  }


  @Autowired
  private ArticleCrudService articleCrudService;

  @RequestMapping(value = DOI_TEMPLATE, method = RequestMethod.POST)
  public ResponseEntity<?> create(@RequestParam(FILE_ARG) MultipartFile file,
                                  @PathVariable(DOI_PREFIX) String doiPrefix, @PathVariable(DOI_ID) String doiId)
      throws IOException, FileStoreException {
    String doi = buildDoi(doiPrefix, doiId);
    articleCrudService.create(file, doi);
    return reportOk();
  }

  @RequestMapping(value = DOI_TEMPLATE, method = RequestMethod.GET)
  public ResponseEntity<?> read(@PathVariable(DOI_PREFIX) String doiPrefix, @PathVariable(DOI_ID) String doiId)
      throws FileStoreException {
    String doi = buildDoi(doiPrefix, doiId);
    byte[] fileData = articleCrudService.read(doi);
    return new ResponseEntity<byte[]>(fileData, HttpStatus.OK);

  }

  @RequestMapping(value = DOI_TEMPLATE, method = RequestMethod.PUT)
  public ResponseEntity<?> update(@RequestParam("file") MultipartFile file,
                                  @PathVariable(DOI_PREFIX) String doiPrefix, @PathVariable(DOI_ID) String doiId)
      throws IOException, FileStoreException {
    String doi = buildDoi(doiPrefix, doiId);
    articleCrudService.update(file, doi);
    return reportOk();
  }

  @RequestMapping(value = DOI_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<?> delete(@PathVariable(DOI_PREFIX) String doiPrefix, @PathVariable(DOI_ID) String doiId) throws FileStoreException {
    String doi = buildDoi(doiPrefix, doiId);
    articleCrudService.delete(doi);
    return reportOk();
  }

}
