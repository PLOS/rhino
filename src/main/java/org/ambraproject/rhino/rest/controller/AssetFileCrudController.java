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
import com.google.common.io.ByteStreams;
import com.google.common.net.HttpHeaders;
import org.ambraproject.rhino.identity.ArticleFileIdentifier;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.rest.controller.abstr.DoiBasedCrudController;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.plos.crepo.exceptions.ContentRepoException;
import org.plos.crepo.exceptions.ErrorType;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

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
  private ContentRepoService contentRepoService;

  @Override
  protected String getNamespacePrefix() {
    return ASSET_NAMESPACE;
  }

  @Override
  protected AssetFileIdentity parse(HttpServletRequest request) {
    return AssetFileIdentity.parse(getIdentifier(request));
  }


  private static final String METADATA_PARAM = "metadata";

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
    Optional<String> contentType = Optional.ofNullable(objMeta.getContentType().orNull());
    // In case contentType field is empty, default to what we would have written at ingestion
    response.setHeader(HttpHeaders.CONTENT_TYPE, contentType.orElseGet(() -> id.inferContentType().toString()));

    Optional<String> filename = Optional.ofNullable(objMeta.getDownloadName().orNull());
    // In case downloadName field is empty, default to what we would have written at ingestion
    String contentDisposition = "attachment; filename=" + filename.orElseGet(() -> id.getFileName()); // TODO: 'attachment' is not always correct
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

  /**
   * @deprecated <em>TEMPORARY.</em> To be removed when the versioned data model is fully supported.
   */
  @Deprecated
  @Transactional(readOnly = true)
  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.GET, params = "versionedPreview")
  public void previewFileFromVersionedModel(HttpServletRequest request, HttpServletResponse response,
                                            @RequestParam(value = "type", required = true) String fileType,
                                            @RequestParam(value = "revision", required = false) Integer revisionNumber)
      throws IOException {
    Doi assetId = Doi.create(getIdentifier(request));
    int revisionNumberValue = (revisionNumber == null) ? articleCrudService.getLatestRevision(assetId) : revisionNumber;

    ArticleFileIdentifier fileId = ArticleFileIdentifier.create(assetId, revisionNumberValue, fileType);
    previewFileFromVersionedModel(request, response, fileId);
  }

  void previewFileFromVersionedModel(HttpServletRequest request, HttpServletResponse response,
                                     ArticleFileIdentifier fileId)
      throws IOException {
    RepoObjectMetadata objectMetadata = assetCrudService.getArticleItemFile(fileId);

    // Used only for defaults when objectMetadata does not supply values.
    // We expect objectMetadata to always supply those values.
    // TODO: Refactor serve not to take this argument when no legacy services depend on it.
    AssetFileIdentity assetFileIdentity = null;

    serve(request, response, assetFileIdentity, objectMetadata);
  }


}
