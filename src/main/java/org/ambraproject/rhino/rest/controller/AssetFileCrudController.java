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
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.controller.abstr.DoiBasedCrudController;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.WriteResult;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.util.response.ServletResponseReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

@Controller
public class AssetFileCrudController extends DoiBasedCrudController {

  private static final String ASSET_ROOT = "/assetfiles";
  private static final String ASSET_NAMESPACE = ASSET_ROOT + "/";
  private static final String ASSET_TEMPLATE = ASSET_NAMESPACE + "**";

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private AssetCrudService assetCrudService;

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
   * @throws FileStoreException
   */
  @RequestMapping(value = ASSET_ROOT, method = RequestMethod.POST)
  public void upload(HttpServletRequest request, HttpServletResponse response,
                     @RequestParam(value = DOI_PARAM) String assetDoi,
                     @RequestParam(value = EXTENSION_PARAM) String extension,
                     @RequestParam(value = FILE_PARAM) MultipartFile assetFile)
      throws IOException, FileStoreException {
    AssetFileIdentity fileIdentity = AssetFileIdentity.create(assetDoi, extension);
    WriteResult<ArticleAsset> result;
    InputStream fileContent = null;
    boolean threw = true;
    try {
      fileContent = assetFile.getInputStream();
      result = assetCrudService.upload(fileContent, fileIdentity);
      threw = false;
    } finally {
      Closeables.close(fileContent, threw);
    }

    response.setStatus(result.getStatus().value());
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(request, response);
    assetCrudService.readMetadata(receiver, fileIdentity.forAsset(), MetadataFormat.JSON);
  }

  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.PUT)
  public ResponseEntity<?> overwrite(HttpServletRequest request) throws IOException, FileStoreException {
    AssetFileIdentity id = parse(request);
    InputStream fileContent = null;
    boolean threw = true;
    try {
      fileContent = request.getInputStream();
      assetCrudService.overwrite(fileContent, id);
      threw = false;
    } finally {
      Closeables.close(fileContent, threw);
    }
    return reportOk();
  }

  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response)
      throws IOException, FileStoreException {
    AssetFileIdentity id = parse(request);

    Optional<ArticleIdentity> articleId = id.forArticle();
    Article article = articleId.isPresent()
        ? articleCrudService.findArticleById(articleId.get()) // null if the file is XML, but not the article XML
        : null; // if the file type is not XML
    if (article != null) {

      // We want to set the Last-Modified header appropriately, so that clients can cache article
      // XML if they choose to.  Unfortunately, the filestore interface doesn't have a way to
      // retrieve a file's mtime, so we have to get this info from the ambra DB instead.
      setLastModifiedHeader(response, article.getLastModified());
      if (checkIfModifiedSince(request, article.getLastModified())) {
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
      } else {

        // !checkIfModifiedSince
        response.setStatus(HttpStatus.NOT_MODIFIED.value());
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

  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.GET, params = {METADATA_PARAM})
  public void readMetadata(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    AssetFileIdentity id = parse(request);
    MetadataFormat mf = MetadataFormat.getFromRequest(request);
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(request, response);
    assetCrudService.readFileMetadata(receiver, id, mf);
  }

  @RequestMapping(value = ASSET_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<?> delete(HttpServletRequest request) throws FileStoreException {
    AssetFileIdentity id = parse(request);
    assetCrudService.delete(id);
    return reportOk();
  }

}
