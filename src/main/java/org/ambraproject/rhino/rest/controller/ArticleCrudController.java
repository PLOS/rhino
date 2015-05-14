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

import com.google.common.net.HttpHeaders;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.controller.abstr.ArticleSpaceController;
import org.ambraproject.rhino.service.AnnotationCrudService;
import org.ambraproject.rhino.service.ArticleRevisionService;
import org.ambraproject.rhino.service.impl.RecentArticleQuery;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.rhino.view.article.ArticleCriteria;
import org.ambraproject.rhombat.HttpDateUtil;
import org.plos.crepo.model.RepoObjectMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Controller for _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 */
@Controller
public class ArticleCrudController extends ArticleSpaceController {

  private static final String DATE_PARAM = "date";
  private static final String PUB_STATE_PARAM = "state";
  private static final String SYND_STATUS_PARAM = "syndication";

  private static final String RECENT_PARAM = "since";
  private static final String JOURNAL_PARAM = "journal";
  private static final String MINIMUM_PARAM = "min";
  private static final String TYPE_PARAM = "type";
  private static final String EXCLUDE_PARAM = "exclude";

  @Autowired
  private ArticleRevisionService articleRevisionService;
  @Autowired
  private AnnotationCrudService annotationCrudService;
  @Autowired
  private AssetFileCrudController assetFileCrudController;

  @Transactional(readOnly = true)
  @RequestMapping(value = ARTICLE_ROOT, method = RequestMethod.GET)
  public void listDois(HttpServletRequest request, HttpServletResponse response,
                       @RequestParam(value = DATE_PARAM, required = false) String includeLastModifiedDate,
                       @RequestParam(value = PUB_STATE_PARAM, required = false) String[] pubStates,
                       @RequestParam(value = SYND_STATUS_PARAM, required = false) String[] syndStatuses)
      throws IOException {
    ArticleCriteria articleCriteria = ArticleCriteria.create(asList(pubStates), asList(syndStatuses),
        booleanParameter(includeLastModifiedDate));
    articleCrudService.listDois(articleCriteria).respond(request, response, entityGson);
  }

  /*
   * Null-safe utility method for Arrays.asList. Put somewhere for reuse?
   */
  private static <E> List<E> asList(E[] array) {
    return (array == null) ? null : Arrays.asList(array);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = ARTICLE_ROOT, params = {RECENT_PARAM, JOURNAL_PARAM}, method = RequestMethod.GET)
  public void listRecent(HttpServletRequest request, HttpServletResponse response,
                         @RequestParam(value = RECENT_PARAM, required = true) String since,
                         @RequestParam(value = JOURNAL_PARAM, required = true) String journalKey,
                         @RequestParam(value = MINIMUM_PARAM, required = false) Integer minimum,
                         @RequestParam(value = TYPE_PARAM, required = false) String[] articleTypes,
                         @RequestParam(value = EXCLUDE_PARAM, required = false) String[] typesToExclude)
      throws IOException {
    RecentArticleQuery query = RecentArticleQuery.builder()
        .setJournalKey(journalKey)
        .setThreshold(HttpDateUtil.parse(since))
        .setArticleTypes(asList(articleTypes))
        .setExcludedArticleTypes(asList(typesToExclude))
        .setMinimum(minimum == null || minimum == 0 ? null : minimum)
        .build();
    articleCrudService.listRecent(query).respond(request, response, entityGson);
  }


  /**
   * Repopulates article category information by making a call to the taxonomy server.
   *
   * @param request  HttpServletRequest
   * @param response HttpServletResponse
   * @throws IOException
   */
  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = ARTICLE_ROOT, method = RequestMethod.POST,
      params = "repopulateCategories")
  public void repopulateCategories(HttpServletRequest request, HttpServletResponse response,
                                   @RequestParam(value = ID_PARAM, required = true) String id)
      throws IOException {
    ArticleIdentity articleIdentity = parse(id, null, null);

    articleCrudService.repopulateCategories(articleIdentity);

    response.setStatus(HttpStatus.ACCEPTED.value());

    // Report the current categories
    articleCrudService.readCategories(articleIdentity).respond(request, response, entityGson);
  }

  /**
   * Retrieves metadata about an article.
   *
   * @param request  HttpServletRequest
   * @param response HttpServletResponse
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = ARTICLE_ROOT, method = RequestMethod.GET, params = ID_PARAM)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam(value = ID_PARAM, required = true) String id,
                   @RequestParam(value = VERSION_PARAM, required = false) Integer versionNumber,
                   @RequestParam(value = REVISION_PARAM, required = false) Integer revisionNumber)
      throws IOException {
    articleCrudService.readMetadata(parse(id, versionNumber, revisionNumber)).respond(request, response, entityGson);
  }

  @Transactional(readOnly = false)
  @RequestMapping(value = ARTICLE_ROOT, method = RequestMethod.POST, params = {ID_PARAM, "legacyReingest"})
  public void legacyReingest(HttpServletRequest request, HttpServletResponse response,
                             @RequestParam(value = ID_PARAM, required = true) String id,
                             @RequestParam(value = VERSION_PARAM, required = false) Integer versionNumber,
                             @RequestParam(value = REVISION_PARAM, required = false) Integer revisionNumber)
      throws IOException {
    Article article = articleCrudService.writeToLegacy(parse(id, versionNumber, revisionNumber));
    articleCrudService.readMetadata(article, false).respond(request, response, entityGson);
  }

  /**
   * Retrieves a list of objects representing comments associated with the article. Each comment has a "replies" list
   * that contains any replies (recursively).
   *
   * @param request
   * @param response
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = ARTICLE_ROOT, method = RequestMethod.GET, params = {ID_PARAM, "comments"})
  public void readComments(HttpServletRequest request, HttpServletResponse response,
                           @RequestParam(value = ID_PARAM, required = true) String id,
                           @RequestParam(value = VERSION_PARAM, required = false) Integer versionNumber,
                           @RequestParam(value = REVISION_PARAM, required = false) Integer revisionNumber)
      throws IOException {
    annotationCrudService.readComments(parse(id, versionNumber, revisionNumber)).respond(request, response, entityGson);
  }

  /**
   * Retrieves a list of objects representing the authors of the article. While the article metadata contains author
   * names, this list will contain more author information than the article metadata, such as author affiliations,
   * corresponding author, etc.
   *
   * @param request
   * @param response
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = ARTICLE_ROOT, method = RequestMethod.GET, params = {ID_PARAM, "authors"})
  public void readAuthors(HttpServletRequest request, HttpServletResponse response,
                          @RequestParam(value = ID_PARAM, required = true) String id,
                          @RequestParam(value = VERSION_PARAM, required = false) Integer versionNumber,
                          @RequestParam(value = REVISION_PARAM, required = false) Integer revisionNumber)
      throws IOException {
    articleCrudService.readAuthors(parse(id, versionNumber, revisionNumber)).respond(request, response, entityGson);
  }

  /**
   * Retrieves the XML file containing the text of an article.
   *
   * @param request
   * @param response
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = ARTICLE_ROOT, method = RequestMethod.GET, params = {ID_PARAM, "xml"})
  public void readXml(HttpServletRequest request, HttpServletResponse response,
                      @RequestParam(value = ID_PARAM, required = true) String id,
                      @RequestParam(value = VERSION_PARAM, required = false) Integer versionNumber,
                      @RequestParam(value = REVISION_PARAM, required = false) Integer revisionNumber)
      throws IOException {
    ArticleIdentity articleIdentity = parse(id, versionNumber, revisionNumber);
    RepoObjectMetadata manuscript = articleRevisionService.getManuscript(articleIdentity);
    streamRepoObject(response, manuscript);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = ARTICLE_ROOT, method = RequestMethod.GET, params = {ID_PARAM, "repack"})
  public void repack(HttpServletResponse response,
                     @RequestParam(value = ID_PARAM, required = true) String id,
                     @RequestParam(value = VERSION_PARAM, required = false) Integer versionNumber,
                     @RequestParam(value = REVISION_PARAM, required = false) Integer revisionNumber)
      throws IOException {
    ArticleIdentity articleIdentity = parse(id, versionNumber, revisionNumber);
    Archive repacked = articleCrudService.readArchive(articleIdentity);

    response.setContentType(MediaType.APPLICATION_XML.toString());
    response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "filename=" + repacked.getArchiveName());
    try (OutputStream stream = new BufferedOutputStream(response.getOutputStream())) {
      repacked.write(stream);
    }
  }

  /**
   * Retrieves a list of objects representing categories associated with the article.
   *
   * @param request
   * @param response
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = ARTICLE_ROOT, method = RequestMethod.GET, params = "categories")
  public void readCategories(HttpServletRequest request, HttpServletResponse response,
                             @RequestParam(value = ID_PARAM, required = true) String id)
      throws IOException {
    ArticleIdentity articleIdentity = parse(id, null, null);
    articleCrudService.readCategories(articleIdentity).respond(request, response, entityGson);
  }

  /**
   * Retrieves a list of objects representing raw taxonomy categories associated with the article.
   *
   * @param request
   * @param response
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = ARTICLE_ROOT, method = RequestMethod.GET, params = "rawCategories")
  public void getRawCategories(HttpServletRequest request, HttpServletResponse response,
                               @RequestParam(value = ID_PARAM, required = true) String id)
      throws IOException {
    ArticleIdentity articleIdentity = parse(id, null, null);
    articleCrudService.getRawCategories(articleIdentity).respond(request, response, entityGson);
  }

}
