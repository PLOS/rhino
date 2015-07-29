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
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import com.google.common.net.HttpHeaders;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.controller.abstr.DoiBasedCrudController;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.WriteResult;
import org.plos.crepo.exceptions.ContentRepoException;
import org.plos.crepo.exceptions.ErrorType;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.service.ContentRepoService;
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
import java.io.OutputStream;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.List;

import static org.ambraproject.rhino.service.impl.AmbraService.reportNotFound;

@Controller
public class AssetFileCrudController extends DoiBasedCrudController {

  private static final String ASSET_ROOT = "/assetfiles";
  private static final String ASSET_NAMESPACE = ASSET_ROOT + "/";
  private static final String ASSET_TEMPLATE = ASSET_NAMESPACE + "**";

  @Autowired
  private AssetCrudService assetCrudService;
  @Autowired
  private ContentRepoService contentRepoService;

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
    RepoObjectMetadata objMeta;
    try {
      objMeta = contentRepoService.getLatestRepoObjectMetadata(id.toString());
    } catch (ContentRepoException e) {
      if (e.getErrorType() == ErrorType.ErrorFetchingObjectMeta) {
        throw reportNotFound(id);
      } else {
        throw e;
      }
    }

    serve(request, response, id, objMeta);
  }

  private void serve(HttpServletRequest request, HttpServletResponse response,
                     AssetFileIdentity id, RepoObjectMetadata objMeta)
      throws IOException {
    Optional<String> contentType = objMeta.getContentType();
    // In case contentType field is empty, default to what we would have written at ingestion
    response.setHeader(HttpHeaders.CONTENT_TYPE, contentType.or(id.inferContentType().toString()));

    Optional<String> filename = objMeta.getDownloadName();
    // In case downloadName field is empty, default to what we would have written at ingestion
    String contentDisposition = "attachment; filename=" + filename.or(id.getFileName()); // TODO: 'attachment' is not always correct
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);

    Timestamp timestamp = objMeta.getTimestamp();
    setLastModifiedHeader(response, timestamp);
    if (!checkIfModifiedSince(request, timestamp)) {
      response.setStatus(HttpStatus.NOT_MODIFIED.value());
      return;
    }

    List<URL> reproxyUrls = objMeta.getReproxyUrls();
    if (clientSupportsReproxy(request) && reproxyUrls != null && !reproxyUrls.isEmpty()) {
      String reproxyUrlHeader = REPROXY_URL_JOINER.join(reproxyUrls);

      response.setStatus(HttpStatus.OK.value());
      response.setHeader("X-Reproxy-URL", reproxyUrlHeader);
      response.setHeader("X-Reproxy-Cache-For", REPROXY_CACHE_FOR_HEADER);
    } else {
      try (InputStream fileStream = contentRepoService.getRepoObject(objMeta.getVersion());
           OutputStream responseStream = response.getOutputStream()) {
        ByteStreams.copy(fileStream, responseStream);
      }
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

  /**
   * @deprecated <em>TEMPORARY.</em> To be removed when the versioned data model is fully supported.
   */
  @Deprecated
  @Transactional(readOnly = true)
  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.GET, params = "versionedPreview")
  public void previewFileFromVersionedModel(
      HttpServletRequest request, HttpServletResponse response,
      @RequestParam(value = "type", required = true) String fileType,
      @RequestParam(value = "version", required = false) Integer versionNumber)
      throws IOException {
    AssetIdentity assetId = AssetIdentity.create(getIdentifier(request));
    ArticleIdentity parentArticle = assetCrudService.getParentArticle(assetId);
    if (parentArticle == null) {
      throw new RestClientException("Asset ID not mapped to article", HttpStatus.NOT_FOUND);
    }

    RepoObjectMetadata assetObject = assetCrudService.getAssetObject(
        parentArticle, assetId, Optional.fromNullable(versionNumber), fileType);

    // TODO: Factor out of 'serve'. This shouldn't need to exist.
    AssetFileIdentity dummyAssetFileIdentity = AssetFileIdentity.parse(assetObject.getDownloadName().get());

    serve(request, response, dummyAssetFileIdentity, assetObject);
  }

}
