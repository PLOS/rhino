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
import com.google.common.io.Closeables;
import org.ambraproject.admin.service.AssetCrudService;
import org.ambraproject.admin.service.DoiBasedCrudService.WriteResult;
import org.ambraproject.filestore.FileStoreException;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

@Controller
public class AssetCrudController extends DoiBasedCrudController {

  private static final String ASSET_NAMESPACE = "/asset/";
  private static final String ASSET_TEMPLATE = ASSET_NAMESPACE + "**";
  private static final String PARENT_PARAM = "assetOf";

  @Autowired
  private AssetCrudService assetCrudService;

  @Override
  protected String getNamespacePrefix() {
    return ASSET_NAMESPACE;
  }


  /**
   * Dispatch an action to upload an asset.
   *
   * @param request  the HTTP request from a REST client
   * @param parentId the DOI of the asset's parent article; required only if the asset is being newly created
   * @return the HTTP response, to indicate success or describe an error
   * @throws IOException
   * @throws FileStoreException
   */
  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.PUT)
  public ResponseEntity<?> upload(HttpServletRequest request,
                                  @RequestParam(value = PARENT_PARAM, required = false) String parentId)
      throws IOException, FileStoreException {
    DoiBasedIdentity assetId = parse(request);
    Optional<DoiBasedIdentity> articleId = (parentId == null)
        ? Optional.<DoiBasedIdentity>absent()
        : Optional.of(DoiBasedIdentity.forArticle(parentId));

    InputStream stream = null;
    WriteResult result;
    try {
      stream = request.getInputStream();
      result = assetCrudService.upload(stream, assetId, articleId);
    } finally {
      Closeables.closeQuietly(stream);
    }
    return new ResponseEntity<Object>(result.getStatus());
  }

  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.GET)
  public ResponseEntity<?> read(HttpServletRequest request) throws FileStoreException, IOException {
    DoiBasedIdentity id = parse(request);

    InputStream fileStream = null;
    byte[] fileData;
    try {
      fileStream = assetCrudService.read(id);
      fileData = IOUtils.toByteArray(fileStream); // TODO Avoid dumping into memory?
    } finally {
      Closeables.closeQuietly(fileStream);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(id.getContentType());

    return new ResponseEntity<byte[]>(fileData, headers, HttpStatus.OK);
  }

  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<?> delete(HttpServletRequest request) throws FileStoreException {
    DoiBasedIdentity id = parse(request);
    assetCrudService.delete(id);
    return reportOk();
  }

}
