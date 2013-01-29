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

import com.google.common.base.Optional;
import com.google.common.io.Closeables;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.controller.abstr.DoiBasedCrudController;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.DoiBasedCrudService.WriteResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

@Controller
public class AssetCrudController extends DoiBasedCrudController<AssetIdentity> {

  private static final String ASSET_NAMESPACE = "/asset/";
  private static final String ASSET_TEMPLATE = ASSET_NAMESPACE + "**";
  private static final String PARENT_PARAM = "assetOf";

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private AssetCrudService assetCrudService;

  @Override
  protected String getNamespacePrefix() {
    return ASSET_NAMESPACE;
  }

  @Override
  protected AssetIdentity parse(HttpServletRequest request) {
    return AssetIdentity.parse(getIdentifier(request));
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
    AssetIdentity assetId = parse(request);
    Optional<ArticleIdentity> articleId = (parentId == null)
        ? Optional.<ArticleIdentity>absent()
        : Optional.of(ArticleIdentity.create(parentId));

    InputStream stream = null;
    WriteResult result;
    boolean threw = true;
    try {
      stream = request.getInputStream();
      result = assetCrudService.upload(stream, assetId, articleId);
      threw = false;
    } finally {
      Closeables.close(stream, threw);
    }
    return respondWithStatus(result.getStatus());
  }

  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response)
      throws IOException, FileStoreException {
    AssetIdentity id = parse(request);

    Optional<ArticleIdentity> articleId = id.forArticle();
    if (articleId.isPresent()) {
      try {
        provideXmlFor(response, articleId.get());
        return;
      } catch (RestClientException e) {
        /*
         * If there was no such article, it might still be a regular asset whose type happens to be XML.
         * Fall through and attempt to serve it as such. (If it isn't there either, the client will get
         * the same "not found" response anyway.)
         */

        if (!HttpStatus.NOT_FOUND.equals(e.getResponseStatus())) {
          throw e; // Anything other than "not found" is unexpected
        }
      }
    }

    InputStream fileStream = null;
    boolean threw = true;
    try {
      fileStream = assetCrudService.read(id);
      respondWithStream(fileStream, response, id);
      threw = false;
    } finally {
      Closeables.close(fileStream, threw);
    }
  }

  /**
   * Write a response containing the XML file for an article.
   *
   * @param response the response object to modify
   * @param article  the parent article of the XML file to send
   * @throws FileStoreException
   * @throws IOException
   */
  private void provideXmlFor(HttpServletResponse response, ArticleIdentity article)
      throws FileStoreException, IOException {
    InputStream fileStream = null;
    boolean threw = true;
    try {
      fileStream = articleCrudService.readXml(article);
      respondWithStream(fileStream, response, article.forXmlAsset());
      threw = false;
    } finally {
      Closeables.close(fileStream, threw);
    }
  }

  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<?> delete(HttpServletRequest request) throws FileStoreException {
    AssetIdentity id = parse(request);
    assetCrudService.delete(id);
    return reportOk();
  }

}
