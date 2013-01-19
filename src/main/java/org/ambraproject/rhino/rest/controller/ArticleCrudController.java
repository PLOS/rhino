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
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.controller.abstr.DoiBasedCrudController;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.DoiBasedCrudService.WriteMode;
import org.ambraproject.rhino.service.DoiBasedCrudService.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

/**
 * Controller for _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 */
@Controller
public class ArticleCrudController extends DoiBasedCrudController<ArticleIdentity> {

  private static final Logger log = LoggerFactory.getLogger(ArticleCrudController.class);

  private static final String ARTICLE_ROOT = "/article";
  private static final String ARTICLE_NAMESPACE = ARTICLE_ROOT + '/';
  private static final String ARTICLE_TEMPLATE = ARTICLE_NAMESPACE + "**";

  /**
   * The request parameter whose value is the XML file being uploaded for a create operation.
   */
  private static final String FILE_ARG = "file";

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private AssetCrudService assetCrudService;

  @Override
  protected String getNamespacePrefix() {
    return ARTICLE_NAMESPACE;
  }

  @Override
  protected ArticleIdentity parse(HttpServletRequest request) {
    return ArticleIdentity.create(getIdentifier(request));
  }


  /**
   * Create an article received at the root noun, without an identifier in the URL.
   * <p/>
   * TODO: Handle the case where the article already exists
   *
   * @param requestFile
   * @return
   */
  @RequestMapping(value = ARTICLE_ROOT, method = RequestMethod.POST)
  public ResponseEntity<?> create(@RequestParam(FILE_ARG) MultipartFile requestFile) throws IOException, FileStoreException {
    InputStream requestBody = null;
    boolean threw = true;
    try {
      requestBody = requestFile.getInputStream();
      articleCrudService.write(requestBody, Optional.<ArticleIdentity>absent(), WriteMode.CREATE_ONLY);
      threw = false;
    } finally {
      Closeables.close(requestBody, threw);
    }
    return reportCreated();
  }

  /**
   * Dispatch an action to upload an article.
   *
   * @param request the HTTP request from a REST client
   * @return the HTTP response, to indicate success or describe an error
   * @throws IOException
   * @throws FileStoreException
   */
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.PUT)
  public ResponseEntity<?> upload(HttpServletRequest request)
      throws IOException, FileStoreException {
    ArticleIdentity id = parse(request);
    InputStream stream = null;
    WriteResult result;
    boolean threw = true;
    try {
      stream = request.getInputStream();
      result = articleCrudService.write(stream, Optional.of(id), WriteMode.WRITE_ANY);
      threw = false;
    } finally {
      Closeables.close(stream, threw);
    }
    return respondWithStatus(result.getStatus());
  }

  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.GET)
  public ResponseEntity<?> read(HttpServletRequest request,
                                @RequestParam(value = METADATA_FORMAT_PARAM, required = false) String format)
      throws FileStoreException, IOException {
    ArticleIdentity id = parse(request);
    MetadataFormat mf = MetadataFormat.getFromParameter(format, true);
    String metadata = articleCrudService.readMetadata(id, mf);
    return respondWithPlainText(metadata);
  }

  /**
   * Send a response containing the XML file for an article.
   * <p/>
   * The API doesn't currently provide this functionality in the article namespace.
   *
   * @param article the parent article of the XML file to send
   * @return the response entity with the XML file stream
   * @throws FileStoreException
   * @throws IOException
   */
  private ResponseEntity<?> provideXmlFor(ArticleIdentity article) throws FileStoreException, IOException {
    InputStream fileStream = null;
    ResponseEntity<byte[]> response;
    boolean threw = true;
    try {
      fileStream = articleCrudService.read(article);
      response = respondWithStream(fileStream, article.forXmlAsset());
      threw = false;
    } finally {
      Closeables.close(fileStream, threw);
    }
    return response;
  }

  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<?> delete(HttpServletRequest request) throws FileStoreException {
    ArticleIdentity id = parse(request);
    articleCrudService.delete(id);
    return reportOk();
  }

}
