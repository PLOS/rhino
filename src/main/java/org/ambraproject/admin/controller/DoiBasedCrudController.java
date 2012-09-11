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

import org.ambraproject.admin.service.DoiBasedCrudService;
import org.ambraproject.filestore.FileStoreException;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

/**
 * Controller for _c_reate, _r_ead, _u_pdate, and _d_elete operations on entities identified by a {@link
 * DoiBasedIdentity}.
 */
public abstract class DoiBasedCrudController extends RestController {

  /**
   * The request parameter name for uploading a single file. This is part of the public REST API.
   */
  protected static final String FILE_ARG = "file";

  /**
   * Return a service object that can perform CRUD operations on the appropriate type of entity. Typically, this is just
   * a constant, dependency-injected field.
   *
   * @return the service
   */
  protected abstract DoiBasedCrudService getService();

  /**
   * Return the URL prefix that describes the RESTful namespace that this controller handles. It should include a
   * leading and trailing slash. Typically this is a constant.
   *
   * @return the constant URL prefix
   */
  protected abstract String getNamespacePrefix();

  private DoiBasedIdentity parse(HttpServletRequest request) {
    return DoiBasedIdentity.parse(request, getNamespacePrefix());
  }


  /*
   * Subclasses should override the CRUD methods below, to make them public and to add a @RequestMapping annotation
   * (and @RequestParam where needed).
   */

  /**
   * Dispatch a "create" action to the service.
   *
   * @param request the HTTP request from a REST client
   * @param file    the uploaded file to use to create an entity
   * @return the HTTP response, to indicate success or describe an error
   * @throws IOException
   * @throws FileStoreException
   */
  protected ResponseEntity<?> create(HttpServletRequest request, MultipartFile file)
      throws IOException, FileStoreException {
    DoiBasedIdentity id = parse(request);
    InputStream stream = null;
    try {
      stream = file.getInputStream();
      getService().create(stream, id);
    } finally {
      IOUtils.closeQuietly(stream);
    }
    return new ResponseEntity<Object>(HttpStatus.CREATED);
  }

  /**
   * Dispatch a "read" action to the service.
   *
   * @param request the HTTP request from a REST client
   * @return the HTTP response containing the read data or describing an error
   * @throws FileStoreException
   * @throws IOException
   */
  protected ResponseEntity<?> read(HttpServletRequest request) throws FileStoreException, IOException {
    DoiBasedIdentity id = parse(request);

    InputStream fileStream = null;
    byte[] fileData;
    try {
      fileStream = getService().read(id);
      fileData = IOUtils.toByteArray(fileStream); // TODO Avoid dumping into memory?
    } finally {
      IOUtils.closeQuietly(fileStream);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(id.getContentType());

    return new ResponseEntity<byte[]>(fileData, headers, HttpStatus.OK);
  }

  /**
   * Dispatch an "update" action to the service.
   *
   * @param request the HTTP request from a REST client
   * @param file    the uploaded file to replace existing data in the file store
   * @return the HTTP response, to indicate success or describe an error
   * @throws IOException
   * @throws FileStoreException
   */
  protected ResponseEntity<?> update(HttpServletRequest request, MultipartFile file)
      throws IOException, FileStoreException {
    DoiBasedIdentity id = parse(request);
    InputStream stream = null;
    try {
      stream = file.getInputStream();
      getService().update(stream, id);
    } finally {
      IOUtils.closeQuietly(stream);
    }
    return reportOk();
  }

  /**
   * Dispatch a "delete" action to the service.
   *
   * @param request the HTTP request from a REST client
   * @return the HTTP response, to indicate success or describe an error
   * @throws FileStoreException
   */
  protected ResponseEntity<?> delete(HttpServletRequest request) throws FileStoreException {
    DoiBasedIdentity id = parse(request);
    getService().delete(id);
    return reportOk();
  }

}
