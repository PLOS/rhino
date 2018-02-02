/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.rest.controller;

import com.google.common.collect.ImmutableMap;
import com.wordnik.swagger.annotations.ApiImplicitParam;
import com.wordnik.swagger.annotations.ApiImplicitParams;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.Category;
import org.ambraproject.rhino.model.Syndication;
import org.ambraproject.rhino.rest.DoiEscaping;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleListCrudService;
import org.ambraproject.rhino.service.ArticleRevisionWriteService;
import org.ambraproject.rhino.service.CommentCrudService;
import org.ambraproject.rhino.service.SolrIndexService;
import org.ambraproject.rhino.service.SyndicationCrudService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.ambraproject.rhino.view.article.ArticleRevisionView;
import org.ambraproject.rhino.view.article.RelationshipSetView;
import org.ambraproject.rhino.view.article.SyndicationInputView;
import org.ambraproject.rhino.view.article.SyndicationView;
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
import org.springframework.web.bind.annotation.RequestHeader;
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  public void listDois(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    // TODO: Reimplement?
    throw new RestClientException("GET /articles not currently supported", HttpStatus.METHOD_NOT_ALLOWED);
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
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/ingestions/{number}", method = RequestMethod.GET)
  public ResponseEntity<?> read(@RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) Date ifModifiedSince,
                                @PathVariable("doi") String doi,
                                @PathVariable("number") int ingestionNumber)
      throws IOException {
    ArticleIngestionIdentifier ingestionId = ArticleIngestionIdentifier.create(DoiEscaping.unescape(doi), ingestionNumber);
    return articleCrudService.serveMetadata(ingestionId).getIfModified(ifModifiedSince).asJsonResponse(entityGson);
  }

  @Transactional()
  @RequestMapping(value = "/articles/{doi}/ingestions/{number}", method = RequestMethod.POST)
  public ResponseEntity<?> updatePreprintDoi(@RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) Date ifModifiedSince,
                                             @PathVariable("doi") String doi,
                                             @PathVariable("number") int ingestionNumber,
                                             @RequestParam("preprintOfDoi") String preprintOfDoi)
      throws IOException {
    ArticleIngestionIdentifier ingestionId =
        ArticleIngestionIdentifier.create(DoiEscaping.unescape(doi), ingestionNumber);

    articleCrudService.updatePreprintDoi(ingestionId, preprintOfDoi);

    return articleCrudService.serveMetadata(ingestionId).getIfModified(ifModifiedSince).asJsonResponse(entityGson);
  }

  @Transactional()
  @RequestMapping(value = "/articles/{doi}/ingestions/{number}", method = RequestMethod.DELETE)
  public ResponseEntity<?> removePreprintDoi(@PathVariable("doi") String doi,
                                             @PathVariable("number") int ingestionNumber)
      throws IOException {
    ArticleIngestionIdentifier ingestionId =
        ArticleIngestionIdentifier.create(DoiEscaping.unescape(doi), ingestionNumber);

    articleCrudService.updatePreprintDoi(ingestionId, null);

    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi:.+}", method = RequestMethod.GET)
  public ResponseEntity<?> getRevisions(@PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier id = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    return articleCrudService.serveOverview(id).asJsonResponse(entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/revisions", method = RequestMethod.GET)
  public ResponseEntity<?> readRevisions(@PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier id = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    return articleCrudService.serveRevisions(id).asJsonResponse(entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/revisions/{revision}", method = RequestMethod.GET)
  public ResponseEntity<?> readRevision(@RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) Date ifModifiedSince,
                                        @PathVariable("doi") String doi,
                                        @PathVariable(value = "revision") Integer revisionNumber)
      throws IOException {
    ArticleRevisionIdentifier id = ArticleRevisionIdentifier.create(DoiEscaping.unescape(doi), revisionNumber);
    return articleCrudService.serveRevision(id).getIfModified(ifModifiedSince).asJsonResponse(entityGson);
  }

  @Transactional(readOnly = false)
  @RequestMapping(value = "/articles/{doi}/revisions", method = RequestMethod.POST)
  public ResponseEntity<?> writeRevision(@PathVariable("doi") String doi,
                                         @RequestParam(value = "revision", required = false) Integer revisionNumber,
                                         @RequestParam(value = "ingestion", required = true) Integer ingestionNumber)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    ArticleIngestionIdentifier ingestionId = ArticleIngestionIdentifier.create(articleId, ingestionNumber);

    final ArticleRevision revision;
    if (revisionNumber == null) {
      revision = articleRevisionWriteService.createRevision(ingestionId);
    } else {
      ArticleRevisionIdentifier revisionId = ArticleRevisionIdentifier.create(articleId, revisionNumber);
      revision = articleRevisionWriteService.writeRevision(revisionId, ingestionId);
    }

    return ServiceResponse.reportCreated(ArticleRevisionView.getView(revision)).asJsonResponse(entityGson);
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
  public ResponseEntity<?> readItems(@RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) Date ifModifiedSince,
                                     @PathVariable("doi") String doi,
                                     @PathVariable("number") int ingestionNumber)
      throws IOException {
    ArticleIngestionIdentifier ingestionId = ArticleIngestionIdentifier.create(DoiEscaping.unescape(doi), ingestionNumber);
    return articleCrudService.serveItems(ingestionId).getIfModified(ifModifiedSince).asJsonResponse(entityGson);
  }

  /**
   * Retrieves a list of objects representing comments associated with the article. Each comment has a "replies" list
   * that contains any replies (recursively).
   *
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/comments", method = RequestMethod.GET)
  public ResponseEntity<?> readComments(@PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier id = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    return commentCrudService.serveComments(id).asJsonResponse(entityGson);
  }

  // TODO: Get rid of this?
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi:.+}/comments", method = RequestMethod.GET, params = "count")
  @ApiImplicitParam(name = "count", value = "count flag (any value)", required = true,
      defaultValue = "count", paramType = "query", dataType = "string")
  public ResponseEntity<?> getCommentCount(@PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier id = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    Article article = articleCrudService.readArticle(id);
    return commentCrudService.getCommentCount(article).asJsonResponse(entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/relationships", method = RequestMethod.GET)
  public ResponseEntity<?> readRelationships(HttpServletRequest request, HttpServletResponse response,
                                             @PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier id = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    return ServiceResponse.serveView(relationshipSetViewFactory.getSetView(id)).asJsonResponse(entityGson);
  }

  /**
   * Retrieves a list of objects representing the authors of the article. While the article metadata contains author
   * names, this list will contain more author information than the article metadata, such as author affiliations,
   * corresponding author, etc.
   *
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/ingestions/{number}/authors", method = RequestMethod.GET)
  public ResponseEntity<?> readAuthors(@RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) Date ifModifiedSince,
                                       @PathVariable("doi") String doi,
                                       @PathVariable("number") int ingestionNumber)
      throws IOException {

    ArticleIngestionIdentifier ingestionId = ArticleIngestionIdentifier.create(DoiEscaping.unescape(doi), ingestionNumber);
    return articleCrudService.serveAuthors(ingestionId).getIfModified(ifModifiedSince).asJsonResponse(entityGson);
  }

  /**
   * Populates article category information by making a call to the taxonomy server.
   *
   * @throws IOException
   */
  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = "/articles/{doi}/categories", method = RequestMethod.POST)
  public ResponseEntity<?> populateCategories(@PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    articleCrudService.populateCategories(articleId);

    // Report the current categories
    return articleCrudService.serveCategories(articleId).asJsonResponse(entityGson);
  }

  /**
   * Retrieves a list of objects representing categories associated with the article.
   *
   * @throws IOException
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/categories", method = RequestMethod.GET)
  public ResponseEntity<?> readCategories(@PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    return articleCrudService.serveCategories(articleId).asJsonResponse(entityGson);
  }

  /**
   * Retrieves a list of objects representing raw taxonomy categories associated with the article.
   *
   * @throws IOException
   */
  // TODO: Get rid of this?
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/{doi}/categories", method = RequestMethod.GET, params = "raw")
  @ApiImplicitParam(name = "raw", value = "raw flag (any value)", required = true,
      defaultValue = "raw", paramType = "query", dataType = "string")
  public ResponseEntity<?> getRawCategories(@PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    return articleCrudService.serveRawCategories(articleId).asJsonResponse(entityGson);
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
  @ApiImplicitParam(name = "rawCategoriesAndText", value = "rawCategoriesAndText flag (any value)", required = true,
      defaultValue = "rawCategoriesAndText", paramType = "query", dataType = "string")
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
  @RequestMapping(value = "/articles/{doi}/categories", params = {"flag"}, method = RequestMethod.POST)
  @ResponseBody
  @ApiImplicitParam(name = "flag", value = "category flagged flag (any value)", required = true,
      defaultValue = "flag", paramType = "query", dataType = "string")
  public Map<String, String> flagArticleCategory(@PathVariable("doi") String articleDoi,
                                                 @RequestParam(value = "categoryTerm") String categoryTerm,
                                                 @RequestParam(value = "userId", required = false) String userId,
                                                 @RequestParam(value = "flag") String action)
      throws IOException {
    ArticleIdentifier articleId = ArticleIdentifier.create(DoiEscaping.unescape(articleDoi));
    Article article = articleCrudService.readArticle(articleId);
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
  @ApiImplicitParam(name = "lists", value = "lists flag (any value)", required = true,
      defaultValue = "lists", paramType = "query", dataType = "string")
  public ResponseEntity<?> getContainingLists(@PathVariable("doi") String doi)
      throws IOException {
    ArticleIdentifier id = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    return articleListCrudService.readContainingLists(id).asJsonResponse(entityGson);
  }

  @RequestMapping(value = "/articles/{doi:.+}", params = {"solrIndex"}, method = RequestMethod.POST)
  public ResponseEntity<?> updateSolrIndex(@PathVariable("doi") String doi,
                                           @ApiParam(value = "Enter 'lite' to perform a lite index. Any other value will perform a standard, full index")
                                           @RequestParam(value = "solrIndex", defaultValue = "standard") String solrIndexMode) {
    ArticleIdentifier identifier = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    solrIndexService.updateSolrIndex(identifier, solrIndexMode.equals("lite"));
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @RequestMapping(value = "/articles/{doi:.+}", params = {"solrIndex"}, method = RequestMethod.DELETE)
  @ApiImplicitParam(name = "solrIndex", value = "solrIndex flag (any value)", required = true,
      defaultValue = "solrIndex", paramType = "query", dataType = "string")
  public ResponseEntity<?> removeSolrIndex(@PathVariable("doi") String doi) {
    ArticleIdentifier identifier = ArticleIdentifier.create(DoiEscaping.unescape(doi));
    solrIndexService.removeSolrIndex(identifier);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @RequestMapping(value = "/articles/{doi}/revisions/{number}/syndications", method = RequestMethod.GET)
  public ResponseEntity<?> readSyndications(@PathVariable("doi") String doi,
                                            @PathVariable("number") int revisionNumber)
      throws IOException {
    ArticleRevisionIdentifier revisionId = ArticleRevisionIdentifier.create(DoiEscaping.unescape(doi), revisionNumber);
    List<Syndication> syndications = syndicationCrudService.getSyndications(revisionId);
    // TODO: If revision does not exist, need to respond with 404 instead of empty list?
    List<SyndicationView> views = syndications.stream().map(SyndicationView::new).collect(Collectors.toList());
    return ServiceResponse.serveView(views).asJsonResponse(entityGson);
  }

  @RequestMapping(value = "/articles/{doi}/revisions/{number}/syndications", method = RequestMethod.POST)
  @ApiImplicitParam(name = "body", paramType = "body", dataType = "SyndicationInputView",
      value = "example: {\"targetQueue\": \"activemq:plos.pmc\"}")
  public ResponseEntity<?> createSyndication(HttpServletRequest request,
                                             @PathVariable("doi") String doi,
                                             @PathVariable("number") int revisionNumber)
      throws IOException {
    ArticleRevisionIdentifier revisionId = ArticleRevisionIdentifier.create(DoiEscaping.unescape(doi), revisionNumber);
    SyndicationInputView input = readJsonFromRequest(request, SyndicationInputView.class);

    Syndication syndication = syndicationCrudService.createSyndication(revisionId, input.getTargetQueue());
    return ServiceResponse.reportCreated(new SyndicationView(syndication)).asJsonResponse(entityGson);
  }

  @RequestMapping(value = "/articles/{doi}/revisions/{number}/syndications",
      // Fold into PATCH operation so we can get rid of "?syndicate"?
      method = RequestMethod.POST, params = "syndicate")
  @ApiOperation(value = "syndicate", notes = "Send a syndication message to the queue for processing. " +
      "Will create and add a syndication to the database if none exist for current article and target.")
  @ApiImplicitParams({
      @ApiImplicitParam(name = "syndicate", value = "syndicate flag (any value)", required = true,
          defaultValue = "syndicate", paramType = "query", dataType = "string"),
      @ApiImplicitParam(name = "body", paramType = "body", dataType = "SyndicationInputView",
          value = "example: {\"targetQueue\": \"activemq:plos.pmc\"}")
  })
  public ResponseEntity<?> syndicate(HttpServletRequest request,
                                     @PathVariable("doi") String doi,
                                     @PathVariable("number") int revisionNumber)
      throws IOException {
    ArticleRevisionIdentifier revisionId = ArticleRevisionIdentifier.create(DoiEscaping.unescape(doi), revisionNumber);
    SyndicationInputView input = readJsonFromRequest(request, SyndicationInputView.class);

    Syndication created = syndicationCrudService.syndicate(revisionId, input.getTargetQueue());
    return ServiceResponse.reportCreated(new SyndicationView(created)).asJsonResponse(entityGson);
  }

  @RequestMapping(value = "/articles/{doi}/revisions/{number}/syndications", method = RequestMethod.PATCH)
  @ApiImplicitParam(name = "body", paramType = "body", dataType = "SyndicationInputView",
      value = "example: {\"targetQueue\": \"activemq:plos.pmc\", \"status\": \"FAILURE\", \"errorMessage\": \"failed\"}")
  public ResponseEntity<?> patchSyndication(HttpServletRequest request,
                                            @PathVariable("doi") String doi,
                                            @PathVariable("number") int revisionNumber)
      throws IOException {
    ArticleRevisionIdentifier revisionId = ArticleRevisionIdentifier.create(DoiEscaping.unescape(doi), revisionNumber);
    SyndicationInputView input = readJsonFromRequest(request, SyndicationInputView.class);

    Syndication patched = syndicationCrudService.updateSyndication(revisionId,
        input.getTargetQueue(), input.getStatus(), input.getErrorMessage());
    return ServiceResponse.serveView(new SyndicationView(patched)).asJsonResponse(entityGson);
  }


  /**
   * The following two methods {@link #getDoisPublishedOn} and {@link #getDoisRevisedOn} provide two utility endpoints
   * for our publication workflow. Their main use-case is to ensure that all articles that are to be published on a
   * given date are picked up by the publication scripts.
   */
  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles", method = RequestMethod.GET, params = "published")
  @ApiImplicitParam(name = "published", value = "published flag (any value)", required = true,
      defaultValue = "published", paramType = "query", dataType = "string")
  public ResponseEntity<?> getDoisPublishedOn(@ApiParam(value = "Date Format: yyyy-MM-dd")
                                              @RequestParam(value = "fromDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
                                              @ApiParam(value = "Date Format: yyyy-MM-dd")
                                              @RequestParam(value = "toDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
                                              @RequestParam(value = "bucketName", required = false) String bucketName) throws IOException {
    List<ArticleRevisionView> views = articleCrudService.getArticlesPublishedOn(fromDate, toDate, bucketName)
        .stream().map(ArticleRevisionView::getView)
        .collect(Collectors.toList());
    return ServiceResponse.serveView(views).asJsonResponse(entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles", method = RequestMethod.GET, params = "revised")
  @ApiImplicitParam(name = "revised", value = "revised flag (any value)", required = true,
      defaultValue = "revised", paramType = "query", dataType = "string")
  public ResponseEntity<?> getDoisRevisedOn(@ApiParam(value = "Date Format: yyyy-MM-dd")
                                            @RequestParam(value = "fromDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
                                            @ApiParam(value = "Date Format: yyyy-MM-dd")
                                            @RequestParam(value = "toDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate,
                                            @RequestParam(value = "bucketName", required = false) String bucketName) throws IOException {
    List<ArticleRevisionView> views = articleCrudService.getArticlesRevisedOn(fromDate, toDate, bucketName)
        .stream().map(ArticleRevisionView::getView)
        .collect(Collectors.toList());
    return ServiceResponse.serveView(views).asJsonResponse(entityGson);
  }
}
