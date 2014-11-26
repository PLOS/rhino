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

import com.google.common.base.Joiner;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.rest.controller.abstr.DoiBasedCrudController;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.WriteResult;
import org.ambraproject.rhombat.HttpDateUtil;
import org.plos.crepo.exceptions.ContentRepoException;
import org.plos.crepo.exceptions.ErrorType;
import org.plos.crepo.service.contentRepo.ContentRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Map;

import static org.ambraproject.rhino.service.impl.AmbraService.reportNotFound;

@Controller
public class AssetFileCrudController extends DoiBasedCrudController {

  private static final String ASSET_ROOT = "/assetfiles";
  private static final String ASSET_NAMESPACE = ASSET_ROOT + "/";
  private static final String ASSET_TEMPLATE = ASSET_NAMESPACE + "**";

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private AssetCrudService assetCrudService;

  @Autowired
  protected ContentRepoService contentRepoService;

  @Override
  protected String getNamespacePrefix() {
    return ASSET_NAMESPACE;
  }

  @Override
  protected AssetFileIdentity parse(HttpServletRequest request) {
    return AssetFileIdentity.parse(getIdentifier(request));
  }


  private static final String DOI_PARAM = "doi";
  private static final String EXTENSION_PARAM = "ext";
  private static final String FILE_PARAM = "file";
  private static final String METADATA_PARAM = "metadata";

  /**
   * Dispatch an action to upload a file for an asset. When the client does this action, the asset ought to already
   * exist as a database entity as a result of ingesting its parent article. The action uploads a data blob to put in
   * the file store, and doesn't change any database data defined by article XML.
   *
   * @param assetDoi  the DOI of the asset to receive the file, which should already exist
   * @param assetFile the blob of data to associate with the asset
   * @return
   * @throws IOException
   */
  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = ASSET_ROOT, method = RequestMethod.POST)
  public void upload(HttpServletRequest request, HttpServletResponse response,
                     @RequestParam(value = DOI_PARAM) String assetDoi,
                     @RequestParam(value = EXTENSION_PARAM) String extension,
                     @RequestParam(value = FILE_PARAM) MultipartFile assetFile)
      throws IOException {
    AssetFileIdentity fileIdentity = AssetFileIdentity.create(assetDoi, extension);
    WriteResult<ArticleAsset> result;
    try (InputStream fileContent = assetFile.getInputStream()) {
      result = assetCrudService.upload(fileContent, fileIdentity);
    }

    response.setStatus(result.getStatus().value());
    assetCrudService.readMetadata(fileIdentity.forAsset()).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.PUT)
  public ResponseEntity<?> overwrite(HttpServletRequest request) throws IOException {
    AssetFileIdentity id = parse(request);
    try (InputStream fileContent = request.getInputStream()) {
      assetCrudService.overwrite(fileContent, id);
    }
    return reportOk();
  }

  private static final Joiner REPROXY_URL_JOINER = Joiner.on(' ');
  private static final int REPROXY_CACHE_FOR_VALUE = 6 * 60 * 60; // TODO: Make configurable
  private static final String REPROXY_CACHE_FOR_HEADER =
      REPROXY_CACHE_FOR_VALUE + "; Last-Modified Content-Type Content-Disposition";

  /**
   * Serve an identified asset file.
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    read(request, response, parse(request));
  }

  void read(HttpServletRequest request, HttpServletResponse response, AssetFileIdentity id)
      throws IOException {
    Map<String, Object> objMeta;
    try {
      objMeta = contentRepoService.getRepoObjMetaLatestVersion(id.toString());
    } catch (ContentRepoException e) {
      if (e.getErrorType() == ErrorType.ErrorFetchingObjectMeta) {
        throw reportNotFound(id);
      } else {
        throw e;
      }
    }

    Timestamp timestamp = Timestamp.valueOf((String) objMeta.get("timestamp"));
    setLastModifiedHeader(response, timestamp);
    if (!checkIfModifiedSince(request, timestamp)) {
      response.setStatus(HttpStatus.NOT_MODIFIED.value());
      return;
    }

    String reproxyUrl = (String) objMeta.get("reproxyURL");
    if (clientSupportsReproxy(request) && reproxyUrl != null) {
      response.setStatus(HttpStatus.OK.value());
      setContentHeaders(response, id);
      response.setHeader("X-Reproxy-URL", reproxyUrl);
      response.setHeader("X-Reproxy-Cache-For", REPROXY_CACHE_FOR_HEADER);
    }
    try (InputStream fileStream = assetCrudService.read(id)) {
      respondWithStream(fileStream, response, id);
    }
  }

  private boolean clientSupportsReproxy(HttpServletRequest request) {
    Enumeration headers = request.getHeaders("X-Proxy-Capabilities");
    if (headers == null) {
      return false;
    }
    while (headers.hasMoreElements()) {
      if ("reproxy-file".equals(headers.nextElement())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Write a response containing the XML file for an article.
   *
   * @param response the response object to modify
   * @param article  the parent article of the XML file to send
   * @throws IOException
   */
  private void provideXmlFor(HttpServletResponse response, ArticleIdentity article)
      throws IOException {
    try (InputStream fileStream = articleCrudService.readXml(article)) {
      respondWithStream(fileStream, response, article.forXmlAsset());
    }
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.GET, params = {METADATA_PARAM})
  public void readMetadata(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    AssetFileIdentity id = parse(request);
    assetCrudService.readFileMetadata(id).respond(request, response, entityGson);
  }

  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<?> delete(HttpServletRequest request) {
    AssetFileIdentity id = parse(request);
    assetCrudService.delete(id);
    return reportOk();
  }

}
