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
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.controller.abstr.ArticleSpaceController;
import org.ambraproject.rhino.service.ArticleRevisionService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.IdentityService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.plos.crepo.exceptions.ContentRepoException;
import org.plos.crepo.exceptions.ErrorType;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.model.RepoVersion;
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
import java.util.Map;

import static org.ambraproject.rhino.service.impl.AmbraService.reportNotFound;

@Controller
public class AssetFileCrudController extends ArticleSpaceController {

  private static final String ASSET_ROOT = "/assetfiles";

  @Autowired
  private AssetCrudService assetCrudService;
  @Autowired
  private ContentRepoService contentRepoService;
  @Autowired
  private ArticleRevisionService articleRevisionService;
  @Autowired
  private IdentityService identityService;


  private static final String METADATA_PARAM = "metadata";

  private static final Joiner REPROXY_URL_JOINER = Joiner.on(' ');
  private static final int REPROXY_CACHE_FOR_VALUE = 6 * 60 * 60; // TODO: Make configurable
  private static final String REPROXY_CACHE_FOR_HEADER =
      REPROXY_CACHE_FOR_VALUE + "; Last-Modified Content-Type Content-Disposition";


  @Transactional(readOnly = true)
  @RequestMapping(value = ASSET_ROOT, method = RequestMethod.GET, params = ID_PARAM)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam(value = ID_PARAM, required = true) String id,
                   @RequestParam(value = VERSION_PARAM, required = false) Integer versionNumber,
                   @RequestParam(value = REVISION_PARAM, required = false) Integer revisionNumber,
                   @RequestParam(value = "fileType", required = true) String fileType)
      throws IOException {


    DoiBasedIdentity assetId = DoiBasedIdentity.create(id);
    ArticleIdentity parentArticle = assetCrudService.getParentArticle(assetId);
    if (parentArticle == null) {
      throw new RestClientException("Asset ID not mapped to article", HttpStatus.NOT_FOUND);
    }

    AssetIdentity assetIdentity = identityService.parseAssetId(parentArticle, assetId, fileType, revisionNumber);

    //return the asset
    read(request, response, assetIdentity);

  }

  void read(HttpServletRequest request, HttpServletResponse response, AssetIdentity assetIdentity)
      throws IOException {
    RepoObjectMetadata objMeta;
    try {
      objMeta = contentRepoService.getRepoObjectMetadata(
          RepoVersion.create(assetIdentity.getIdentifier(), assetIdentity.getUuid().get()));
    } catch (ContentRepoException e) {
      if (e.getErrorType() == ErrorType.ErrorFetchingObjectMeta) {
        throw reportNotFound(assetIdentity);
      } else {
        throw e;
      }
    }

    Optional<String> contentType = objMeta.getContentType();
    // In case contentType field is empty, default to what we would have written at ingestion
    response.setHeader(HttpHeaders.CONTENT_TYPE, contentType.or(objMeta.getContentType().get()));

    Optional<String> filename = objMeta.getDownloadName();
    // In case downloadName field is empty, default to what we would have written at ingestion
    String contentDisposition = "attachment; filename=" + filename.or(objMeta.getDownloadName().get()); // TODO: 'attachment' is not always correct
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
      try (InputStream fileStream = assetCrudService.read(assetIdentity);
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
  @RequestMapping(value = ASSET_ROOT, method = RequestMethod.GET, params = {METADATA_PARAM})
  public void readMetadata(HttpServletRequest request, HttpServletResponse response,
                           @RequestParam(ID_PARAM) String id)
      throws IOException {
    assetCrudService.readFileMetadata(AssetFileIdentity.parse(id)).respond(request, response, entityGson);
  }

}
