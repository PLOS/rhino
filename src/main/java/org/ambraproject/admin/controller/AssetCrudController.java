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

import org.ambraproject.admin.service.AssetCrudService;
import org.ambraproject.admin.service.DoiBasedCrudService;
import org.ambraproject.filestore.FileStoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@Controller
public class AssetCrudController extends DoiBasedCrudController {

  private static final String ASSET_NAMESPACE = "/asset/";
  private static final String ASSET_TEMPLATE = ASSET_NAMESPACE + "**";

  @Autowired
  private AssetCrudService assetCrudService;

  @Override
  protected DoiBasedCrudService getService() {
    return assetCrudService;
  }

  @Override
  protected String getNamespacePrefix() {
    return ASSET_NAMESPACE;
  }


  @Override
  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.POST)
  public ResponseEntity<?> create(HttpServletRequest request, @RequestParam(FILE_ARG) MultipartFile file)
      throws IOException, FileStoreException {
    return super.create(request, file);
  }

  @Override
  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.GET)
  public ResponseEntity<?> read(HttpServletRequest request) throws FileStoreException, IOException {
    return super.read(request);
  }

  @Override
  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.PUT)
  public ResponseEntity<?> update(HttpServletRequest request, @RequestParam(FILE_ARG) MultipartFile file)
      throws IOException, FileStoreException {
    return super.update(request, file);
  }

  @Override
  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<?> delete(HttpServletRequest request) throws FileStoreException {
    return super.delete(request);
  }

}
