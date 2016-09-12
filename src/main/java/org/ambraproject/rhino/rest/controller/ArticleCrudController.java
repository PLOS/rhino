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

import com.google.common.collect.ImmutableMap;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.model.ArticleCategoryAssignment;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.Category;
import org.ambraproject.rhino.model.Syndication;
import org.ambraproject.rhino.rest.DoiEscaping;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleListCrudService;
import org.ambraproject.rhino.service.ArticleRevisionWriteService;
import org.ambraproject.rhino.service.CommentCrudService;
import org.ambraproject.rhino.service.SolrIndexService;
import org.ambraproject.rhino.service.SyndicationCrudService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.article.ArticleCriteria;
import org.ambraproject.rhino.view.article.SyndicationInputView;
import org.ambraproject.rhino.view.article.versioned.ArticleRevisionView;
import org.ambraproject.rhino.view.article.versioned.RelationshipSetView;
import org.ambraproject.rhino.view.article.versioned.SyndicationView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

/**
 * Controller for _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 */
@Controller
public class ArticleCrudController extends RestController {

  private static final Logger log = LoggerFactory.getLogger(ArticleCrudController.class);


  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private ArticleRevisionWriteService articleRevisionWriteService;
  @Autowired
  private SolrIndexService solrIndexService;
  @Autowired
  private CommentCrudService commentCrudService;
  @Autowired
  private AssetFileCrudController assetFileCrudController;
  @Autowired
  private ArticleListCrudService articleListCrudService;
  @Autowired
  private SyndicationCrudService syndicationCrudService;
  @Autowired
  private TaxonomyService taxonomyService;
  @Autowired
  private RelationshipSetView.Factory relationshipSetViewFactory;

  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles", method = RequestMethod.GET)
  public void listDois(HttpServletRequest request, HttpServletResponse response,
                       @RequestParam(value = "date", required = false) String includeLastModifiedDate,
                       @RequestParam(value = "state", required = false) String[] pubStates,
                       @RequestParam(value = "syndication", required = false) String[] syndStatuses)
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

  /**
   * Read article metadata.
   *
   * @throws IOException
   */
  @Deprecated
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/ingestions/{number}", method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response,
                   @PathVariable("doi") String doi,
                   @PathVariable("number") int ingestionNumber)
      throws IOException {
    ArticleIngestionIdentifier ingestionId = ArticleIngestionIdentifier.create(DoiEscaping.unescape(doi), ingestionNumber);
    articleCrudService.serveMetadata(ingestionId).respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi:.+}", method = RequestMethod.GET)
  public void getRevisions(HttpServletRequest request, HttpServletResponse response,
                           @PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier id = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    articleCrudService.serveOverview(id).respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/revisions/{revision}", method = RequestMethod.GET)
  public void readRevision(HttpServletRequest request, HttpServletResponse response,
                           @PathVariable("doi") String doi,
                           @PathVariable(value = "revision") Integer revisionNumber)
      throws IOException {
    ArticleRevisionIdentifier id = ArticleRevisionIdentifier.create(DoiEscaping.unescape(doi), revisionNumber);
    articleCrudService.serveRevision(id).respond(request, response, entityGson);
  }

  @Transactional(readOnly = false)
  @RequestMapping(value = "/articles/{doi}/revisions", method = RequestMethod.POST)
  public ResponseEntity<?> writeRevision(@PathVariable("doi") String doi,
                                         @RequestParam(value = "revision", required = false) Integer revisionNumber,
                                         @RequestParam(value = "ingestion", required = true) Integer ingestionNumber) {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    ArticleIngestionIdentifier ingestionId = ArticleIngestionIdentifier.create(articleId, ingestionNumber);

    final ArticleRevision revision;
    if (revisionNumber == null) {
      revision = articleRevisionWriteService.createRevision(ingestionId);
    } else {
      ArticleRevisionIdentifier revisionId = ArticleRevisionIdentifier.create(articleId, revisionNumber);
      revision = articleRevisionWriteService.writeRevision(revisionId, ingestionId);
    }

    return reportCreated(ArticleRevisionView.getView(revision));
  }

  @Transactional(readOnly = false)
  @RequestMapping(value = "/articles/{doi}/revisions/{revision}", method = RequestMethod.DELETE)
  public ResponseEntity<?> deleteRevision(@PathVariable("doi") String doi,
                                          @PathVariable("revision") int revisionNumber) {
    ArticleRevisionIdentifier revisionId = ArticleRevisionIdentifier.create(DoiEscaping.unescape(doi), revisionNumber);
    articleRevisionWriteService.deleteRevision(revisionId);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @RequestMapping(value = "/articles/{doi}/ingestions/{number}/items", method = RequestMethod.GET)
  public void readItems(HttpServletRequest request, HttpServletResponse response,
                        @PathVariable("doi") String doi,
                        @PathVariable("number") int ingestionNumber)
      throws IOException {
    ArticleIngestionIdentifier ingestionId = ArticleIngestionIdentifier.create(DoiEscaping.unescape(doi), ingestionNumber);
    articleCrudService.serveItems(ingestionId).respond(request, response, entityGson);
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
  @RequestMapping(value = "/articles/{doi}/comments", method = RequestMethod.GET)
  public void readComments(HttpServletRequest request, HttpServletResponse response,
                           @PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier id = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    commentCrudService.serveComments(id).respond(request, response, entityGson);
  }

  // TODO: Get rid of this?
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi:.+}", method = RequestMethod.GET, params = "commentCount")
  public void getCommentCount(HttpServletRequest request, HttpServletResponse response,
                              @PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier id = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    ArticleTable article = articleCrudService.readArticle(id);
    commentCrudService.getCommentCount(article).respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/relationships", method = RequestMethod.GET)
  public void readRelationships(HttpServletRequest request, HttpServletResponse response,
                                @PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier id = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    Transceiver.serveUntimestampedView(() -> relationshipSetViewFactory.getSetView(id))
        .respond(request, response, entityGson);
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
  @RequestMapping(value = "/articles/{doi}/ingestions/{number}/authors", method = RequestMethod.GET)
  public void readAuthors(HttpServletRequest request, HttpServletResponse response,
                          @PathVariable("doi") String doi,
                          @PathVariable("number") int ingestionNumber)
      throws IOException {

    ArticleIngestionIdentifier ingestionId = ArticleIngestionIdentifier.create(DoiEscaping.unescape(doi), ingestionNumber);
    articleCrudService.serveAuthors(ingestionId).respond(request, response, entityGson);
  }

  /**
   * Populates article category information by making a call to the taxonomy server.
   *
   * @param request  HttpServletRequest
   * @param response HttpServletResponse
   * @throws IOException
   */
  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/articles/{doi}/categories", method = RequestMethod.POST)
  public void populateCategories(HttpServletRequest request, HttpServletResponse response,
                                 @PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    articleCrudService.populateCategories(articleId);

    // Report the current categories
    articleCrudService.serveCategories(articleId).respond(request, response, entityGson);
  }

  /**
   * Retrieves a list of objects representing categories associated with the article.
   *
   * @param request
   * @param response
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/categories", method = RequestMethod.GET)
  public void readCategories(HttpServletRequest request, HttpServletResponse response,
                             @PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    articleCrudService.serveCategories(articleId).respond(request, response, entityGson);
  }

  /**
   * Retrieves a list of objects representing raw taxonomy categories associated with the article.
   *
   * @param request
   * @param response
   * @throws IOException
   */
  // TODO: Get rid of this?
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/categories", method = RequestMethod.GET, params = "raw")
  public void getRawCategories(HttpServletRequest request, HttpServletResponse response,
                               @PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    articleCrudService.serveRawCategories(articleId).respond(request, response, entityGson);
  }

  /**
   * Retrieves the raw taxonomy categories associated with the article along with the text that is sent to the taxonomy
   * server for classification
   *
   * @param request
   * @return a String containing the text and raw categories in the form of <text> \n\n <categories>
   * @throws IOException
   */
  // TODO: Get rid of this?
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/categories", method = RequestMethod.GET, params = "rawCategoriesAndText")
  public ResponseEntity<String> getRawCategoriesAndText(HttpServletRequest request,
                                                        @PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(doi));

    String categoriesAndText = articleCrudService.getRawCategoriesAndText(articleId);
    HttpHeaders responseHeader = new HttpHeaders();
    responseHeader.setContentType(MediaType.TEXT_HTML);
    return new ResponseEntity<>(categoriesAndText, responseHeader, HttpStatus.OK);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/articles/{doi}/categories", params={"flag"}, method = RequestMethod.POST)
  @ResponseBody
  public Map<String, String> flagArticleCategory(@PathVariable("doi")  String articleDoi,
                                                 @RequestParam(value = "categoryTerm", required = true) String categoryTerm,
                                                 @RequestParam(value = "userId", required = false) String userId,
                                                 @RequestParam(value = "flag", required = true) String action)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(articleDoi));
    ArticleTable article = articleCrudService.readArticle(articleId);
    Optional<Long> userIdObj = Optional.ofNullable(userId).map(Long::parseLong);

    Collection<Category> categories = taxonomyService.getArticleCategoriesWithTerm(article, categoryTerm);

    switch (action) {
      case "add":
        for (Category category : categories) {
          taxonomyService.flagArticleCategory(article, category, userIdObj);
        }
        break;
      case "remove":
        for (Category category : categories) {
          taxonomyService.deflagArticleCategory(article, category, userIdObj);
        }
        break;
      default:
        throw new RestClientException("action must be 'add' or 'remove'", HttpStatus.BAD_REQUEST);
    }

    return ImmutableMap.of(); // ajax call expects returned data so provide an empty map for the body
  }


  /**
   * Retrieves a collection of article lists that contain an article.
   */
  @Transactional(readOnly = true)
  @RequestMapping(
      // Not "/articles/{doi}/lists" because a list isn't a child object of the article. This is kind of a search query.
      value = "/articles/{doi:.+}", method = RequestMethod.GET, params = "lists")
  public void getContainingLists(HttpServletRequest request, HttpServletResponse response,
                                 @PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier id = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    articleListCrudService.readContainingLists(id).respond(request, response, entityGson);
  }

  @RequestMapping(value = "/articles/{doi:.+}", params = {"solrIndex"}, method = RequestMethod.POST)
  public ResponseEntity<?> updateSolrIndex(@PathVariable("doi") String doi) {
    ArticleIdentifier identifier = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    solrIndexService.updateSolrIndex(identifier);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @RequestMapping(value = "/articles/{doi:.+}", params = {"solrIndex"}, method = RequestMethod.DELETE)
  public ResponseEntity<?> removeSolrIndex(@PathVariable("doi") String doi) {
    ArticleIdentifier identifier = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    solrIndexService.removeSolrIndex(identifier);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @RequestMapping(value = "/articles/{doi}/revisions/{number}/syndications", method = RequestMethod.GET)
  public void readSyndications(HttpServletRequest request, HttpServletResponse response,
                               @PathVariable("doi") String doi,
                               @PathVariable("number") int revisionNumber)
      throws IOException {
    Transceiver.serveUntimestampedView(() -> {
      ArticleRevisionIdentifier revisionId = ArticleRevisionIdentifier.create(DoiEscaping.unescape(doi), revisionNumber);
      List<Syndication> syndications = syndicationCrudService.getSyndications(revisionId);
      // TODO: If revision does not exist, need to respond with 404 instead of empty list?
      return syndications.stream().map(SyndicationView::new).collect(Collectors.toList());
    }).respond(request, response, entityGson);
  }

  @RequestMapping(value = "/articles/{doi}/revisions/{number}/syndications", method = RequestMethod.POST)
  public ResponseEntity<String> createSyndication(HttpServletRequest request,
                                                  @PathVariable("doi") String doi,
                                                  @PathVariable("number") int revisionNumber)
      throws IOException {
    ArticleRevisionIdentifier revisionId = ArticleRevisionIdentifier.create(DoiEscaping.unescape(doi), revisionNumber);
    SyndicationInputView input = readJsonFromRequest(request, SyndicationInputView.class);

    Syndication syndication = syndicationCrudService.createSyndication(revisionId, input.getTargetQueue());
    return reportCreated(new SyndicationView(syndication));
  }

  @RequestMapping(value = "/articles/{doi}/revisions/{number}/syndications",
      // Fold into PATCH operation so we can get rid of "?syndicate"?
      method = RequestMethod.POST, params = "syndicate")
  @ApiOperation(value = "syndicate", notes = "Send a syndication message to the queue for processing. " +
      "Will create and add a syndication to the database if none exist for current article and target.")
  public ResponseEntity<?> syndicate(HttpServletRequest request,
                                     @PathVariable("doi") String doi,
                                     @PathVariable("number") int revisionNumber)
      throws IOException {
    ArticleRevisionIdentifier revisionId = ArticleRevisionIdentifier.create(DoiEscaping.unescape(doi), revisionNumber);
    SyndicationInputView input = readJsonFromRequest(request, SyndicationInputView.class);

    Syndication created = syndicationCrudService.syndicate(revisionId, input.getTargetQueue());
    return reportCreated(new SyndicationView(created));
  }

  @RequestMapping(value = "/articles/{doi}/revisions/{number}/syndications", method = RequestMethod.PATCH)
  public ResponseEntity<?> patchSyndication(HttpServletRequest request,
                                            @PathVariable("doi") String doi,
                                            @PathVariable("number") int revisionNumber)
      throws IOException {
    ArticleRevisionIdentifier revisionId = ArticleRevisionIdentifier.create(DoiEscaping.unescape(doi), revisionNumber);
    SyndicationInputView input = readJsonFromRequest(request, SyndicationInputView.class);

    Syndication patched = syndicationCrudService.updateSyndication(revisionId,
        input.getTargetQueue(), input.getStatus(), input.getErrorMessage());
    return reportUpdated(new SyndicationView(patched));
  }

  /**
   * Retrieves the metadata from a random article
   *
   * @param request  HttpServletRequest
   * @param response HttpServletResponse
   * @return a JSON representation of the random article
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles", method = RequestMethod.GET, params = "random")
  public void readRandom(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    articleCrudService.readRandom().respond(request, response, entityGson);
  }


  /**
   * The following two methods {@link getDoisPublishedOn()} and {@link getDoisRevisedOn()} provide two
   * utility endpoints for our publication workflow. Their main use-case is to ensure that all articles
   * that are to be published on a given date are picked up by the publication scripts.
   *
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles", method = RequestMethod.GET, params = "published")
  public void getDoisPublishedOn(HttpServletRequest request, HttpServletResponse response,
                                 @ApiParam(value = "Date Format: yyyy-MM-dd")
                                 @RequestParam(value = "fromDate") @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate fromDate,
                                 @ApiParam(value = "Date Format: yyyy-MM-dd")
                                 @RequestParam(value = "toDate") @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate toDate) throws IOException {
  Transceiver.serveUntimestampedView(() -> articleCrudService.getArticlesPublishedOn(fromDate, toDate)
      .stream().map(ArticleRevisionView::getView)
      .collect(Collectors.toList())).respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles", method = RequestMethod.GET, params = "revised")
  public void getDoisRevisedOn(HttpServletRequest request, HttpServletResponse response,
                               @ApiParam(value = "Date Format: yyyy-MM-dd")
                               @RequestParam(value = "fromDate") @DateTimeFormat(pattern="yyyy-MM-dd")LocalDate fromDate,
                               @ApiParam(value = "Date Format: yyyy-MM-dd")
                               @RequestParam(value = "toDate") @DateTimeFormat(pattern="yyyy-MM-dd") LocalDate toDate) throws IOException {

    Transceiver.serveUntimestampedView(() -> articleCrudService.getArticlesRevisedOn(fromDate, toDate)
        .stream().map(ArticleRevisionView::getView)
        .collect(Collectors.toList())).respond(request, response, entityGson);
  }

}
