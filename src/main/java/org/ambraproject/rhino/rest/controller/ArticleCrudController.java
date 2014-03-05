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
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.controller.abstr.ArticleSpaceController;
import org.ambraproject.rhino.service.AnnotationCrudService;
import org.ambraproject.rhino.service.DoiBasedCrudService.WriteMode;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.util.response.ServletResponseReceiver;
import org.ambraproject.rhino.view.article.ArticleCriteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Controller for _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 */
@Controller
public class ArticleCrudController extends ArticleSpaceController {

  private static final Logger log = LoggerFactory.getLogger(ArticleCrudController.class);

  /**
   * The request parameter whose value is the XML file being uploaded for a create operation.
   */
  private static final String ARTICLE_XML_FIELD = "xml";

  private static final String PUB_STATE_PARAM = "state";
  private static final String SYND_STATUS_PARAM = "syndication";

  @Autowired
  private AnnotationCrudService annotationCrudService;

  @RequestMapping(value = ARTICLE_ROOT, method = RequestMethod.GET)
  public void listDois(HttpServletResponse response,
                       @RequestParam(value = PUB_STATE_PARAM, required = false) String[] pubStates,
                       @RequestParam(value = SYND_STATUS_PARAM, required = false) String[] syndStatuses,
                       @RequestParam(value = JSONP_CALLBACK_PARAM, required = false) String jsonp,
                       @RequestHeader(value = ACCEPT_REQUEST_HEADER, required = false) String accept)
      throws IOException {
    MetadataFormat mf = MetadataFormat.getFromAcceptHeader(accept);
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(jsonp, response);
    ArticleCriteria articleCriteria = ArticleCriteria.create(asList(pubStates), asList(syndStatuses));
    articleCrudService.listDois(receiver, mf, articleCriteria);
  }

  /*
   * Null-safe utility method for Arrays.asList. Put somewhere for reuse?
   */
  private static <E> List<E> asList(E[] array) {
    return (array == null) ? null : Arrays.asList(array);
  }


  /**
   * Create an article received at the root noun, without an identifier in the URL. Respond with the received data.
   *
   * @param response
   * @param requestFile
   * @throws IOException
   * @throws FileStoreException
   */
  @RequestMapping(value = ARTICLE_ROOT, method = RequestMethod.POST)
  public void create(HttpServletResponse response,
                     @RequestParam(ARTICLE_XML_FIELD) MultipartFile requestFile,
                     @RequestParam(value = JSONP_CALLBACK_PARAM, required = false) String jsonp)
      throws IOException, FileStoreException {
    Article result;
    try (InputStream requestBody = requestFile.getInputStream()) {
      result = articleCrudService.write(requestBody, Optional.<ArticleIdentity>absent(), WriteMode.CREATE_ONLY);
    }
    response.setStatus(HttpStatus.CREATED.value());

    // Report the written data, as JSON, in the response.
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(jsonp, response);
    articleCrudService.readMetadata(receiver, result, MetadataFormat.JSON);
  }

  /**
   * Retrieves either metadata about an article (default), or entities associated with an article depending on the
   * parameters.
   *
   * @param request  HttpServletRequest
   * @param response HttpServletResponse
   * @param comments if present, the response will be a list of objects representing comments associated with the
   *                 article, instead of the article metadata. Each comment has a "replies" list that contains any
   *                 replies (recursively).
   * @param authors  if present, the response will be a list of objects representing the authors of the article.  While
   *                 the article metadata contains author names, this list will contain more author information than the
   *                 article metadata, such as author affiliations, corresponding author, etc.
   * @throws FileStoreException
   * @throws IOException
   */
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam(value = "comments", required = false) String comments,
                   @RequestParam(value = "authors", required = false) String authors)
      throws FileStoreException, IOException {
    ArticleIdentity id = parse(request);
    MetadataFormat mf = MetadataFormat.getFromRequest(request);
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(request, response);
    if (booleanParameter(comments)) {
      annotationCrudService.readComments(receiver, id, mf);
    } else if (booleanParameter(authors)) {
      articleCrudService.readAuthors(receiver, id, mf);
    } else {
      articleCrudService.readMetadata(receiver, id, mf);
    }
  }

  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<?> delete(HttpServletRequest request) throws FileStoreException {
    ArticleIdentity id = parse(request);
    articleCrudService.delete(id);
    return reportOk();
  }

}
