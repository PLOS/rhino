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

import org.ambraproject.rhino.identity.ArticleFileIdentifier;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.rest.controller.abstr.ArticleSpaceController;
import org.ambraproject.rhino.service.AnnotationCrudService;
import org.ambraproject.rhino.service.ArticleCrudService.ArticleMetadataSource;
import org.ambraproject.rhino.service.ArticleListCrudService;
import org.ambraproject.rhino.service.impl.RecentArticleQuery;
import org.ambraproject.rhino.view.article.ArticleCriteria;
import org.ambraproject.rhombat.HttpDateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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

  private static final String DATE_PARAM = "date";
  private static final String PUB_STATE_PARAM = "state";
  private static final String SYND_STATUS_PARAM = "syndication";

  private static final String RECENT_PARAM = "since";
  private static final String JOURNAL_PARAM = "journal";
  private static final String MINIMUM_PARAM = "min";
  private static final String TYPE_PARAM = "type";
  private static final String EXCLUDE_PARAM = "exclude";

  @Autowired
  private AnnotationCrudService annotationCrudService;

  @Autowired
  private AssetFileCrudController assetFileCrudController;

  @Autowired
  private ArticleListCrudService articleListCrudService;

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
   * @param request          HttpServletRequest
   * @param response         HttpServletResponse
   * @throws IOException
   */
  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.POST,
      params = "repopulateCategories")
  public void repopulateCategories(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    ArticleIdentity id = parse(request);

    articleCrudService.repopulateCategories(id);

    // Report the current categories
    articleCrudService.readCategories(id).respond(request, response, entityGson);
  }

  /**
   * Retrieves metadata about an article.
   *
   * @param request          HttpServletRequest
   * @param response         HttpServletResponse
   * @param excludeCitations
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @RequestParam(value = "excludeCitations", required = false) boolean excludeCitations)
      throws IOException {
    ArticleIdentity id = parse(request);
    articleCrudService.readMetadata(id, excludeCitations).respond(request, response, entityGson);
  }

  /**
   * Replicates the behavior of {@link #read}, and forces the service to read from the versioned data model. For
   * verification and debugging purposes only, while regular read services don't fully use the versioned data model.
   *
   * @deprecated <em>TEMPORARY.</em> To be removed when the versioned data model is fully supported.
   */
  @Deprecated
  @Transactional(readOnly = true)
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.GET, params = "versionedPreview")
  public void previewMetadataFromVersionedModel(
      HttpServletRequest request, HttpServletResponse response,
      @RequestParam(value = "revision", required = false) Integer revisionNumber,
      @RequestParam(value = "excludeCitations", required = false) boolean excludeCitations,
      @RequestParam(value = "parseFullManuscript", required = false) boolean parseFullManuscript)
      throws IOException {
    ArticleMetadataSource sourceObj = parseFullManuscript ? ArticleMetadataSource.FULL_MANUSCRIPT
        : excludeCitations ? ArticleMetadataSource.FRONT_MATTER
        : ArticleMetadataSource.FRONT_AND_BACK_MATTER;

    Doi id = Doi.create(getIdentifier(request));
    int revisionNumberValue = (revisionNumber == null) ? articleCrudService.getLatestRevision(id) : revisionNumber;
    ArticleRevisionIdentifier versionId = ArticleRevisionIdentifier.create(id, revisionNumberValue);
    articleCrudService.readVersionedMetadata(versionId, sourceObj).respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.GET, params = {"revisions", "versionedPreview"})
  public void getRevisions(HttpServletRequest request, HttpServletResponse response) throws IOException {
    ArticleIdentifier id = ArticleIdentifier.create(getIdentifier(request));
    articleCrudService.readRevisions(id).respond(request, response, entityGson);
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
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.GET, params = "comments")
  public void readComments(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    ArticleIdentity id = parse(request);
    annotationCrudService.readComments(id).respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.GET, params = "commentCount")
  public void getCommentCount(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    ArticleIdentity id = parse(request);
    Article article = articleCrudService.findArticleById(id);
    annotationCrudService.getCommentCount(article).respond(request, response, entityGson);
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
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.GET, params = "authors")
  public void readAuthors(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    ArticleIdentity id = parse(request);
    articleCrudService.readAuthors(id).respond(request, response, entityGson);
  }

  /**
   * Retrieves the XML file containing the text of an article.
   *
   * @param request
   * @param response
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.GET, params = "xml")
  public void readXml(HttpServletRequest request, HttpServletResponse response,
                      @RequestParam(value = "revision", required = false) Integer revisionNumber)
      throws IOException {
    Doi assetId = Doi.create(getIdentifier(request));
    int revisionNumberValue = (revisionNumber == null) ? articleCrudService.getLatestRevision(assetId) : revisionNumber;
    assetFileCrudController.previewFileFromVersionedModel(request, response,
        ArticleFileIdentifier.create(assetId, revisionNumberValue, "manuscript"));
  }

  /**
   * Retrieves a list of objects representing categories associated with the article.
   *
   * @param request
   * @param response
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.GET, params = "categories")
  public void readCategories(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    ArticleIdentity id = parse(request);
    articleCrudService.readCategories(id).respond(request, response, entityGson);
  }

  /**
   * Retrieves a list of objects representing raw taxonomy categories associated with the article.
   *
   * @param request
   * @param response
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.GET, params = "rawCategories")
  public void getRawCategories(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    ArticleIdentity id = parse(request);
    articleCrudService.getRawCategories(id).respond(request, response, entityGson);
  }

  /**
   * Retrieves a collection of article lists that contain an article.
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.GET, params = "lists")
  public void getContainingLists(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    ArticleIdentity id = parse(request);
    articleListCrudService.readContainingLists(id).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.DELETE)
  public ResponseEntity<?> delete(HttpServletRequest request) {
    ArticleIdentity id = parse(request);
    articleCrudService.delete(id);
    return reportOk();
  }

  /**
   * Retrieves the raw taxonomy categories associated with the article along with the text that is sent to the
   * taxonomy server for classification
   *
   * @param request
   * @return a String containing the text and raw categories in the form of <text> \n\n <categories>
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.GET, params = "rawCategoriesAndText")
  public ResponseEntity<String> getRawCategoriesAndText(HttpServletRequest request)
      throws IOException {
    ArticleIdentity id = parse(request);

    String categoriesAndText = articleCrudService.getRawCategoriesAndText(id);
    HttpHeaders responseHeader = new HttpHeaders();
    responseHeader.setContentType(MediaType.TEXT_HTML);
    ResponseEntity<String> response = new ResponseEntity<>(categoriesAndText, responseHeader,
        HttpStatus.OK);
    return response;
  }

  /**
   * Retrieves the metadata from a random article
   *
   * @param request          HttpServletRequest
   * @param response         HttpServletResponse
   * @return a JSON representation of the random article
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = ARTICLE_TEMPLATE, method = RequestMethod.GET, params = "random")
  public void readRandom(HttpServletRequest request, HttpServletResponse response)
      throws IOException {

    articleCrudService.readRandom().respond(request, response, entityGson);
  }

}
