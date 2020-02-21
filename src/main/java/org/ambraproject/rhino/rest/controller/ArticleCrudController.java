/*
 * Copyright (c) 2017-2019 Public Library of Science
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

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.ImmutableMap;
import com.wordnik.swagger.annotations.ApiImplicitParam;
import com.wordnik.swagger.annotations.ApiParam;

import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.Category;
import org.ambraproject.rhino.rest.DoiEscaping;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleListCrudService;
import org.ambraproject.rhino.service.ArticleRevisionWriteService;
import org.ambraproject.rhino.service.CommentCrudService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.ambraproject.rhino.view.article.ArticleRevisionView;
import org.ambraproject.rhino.view.article.RelationshipViewFactory;
import org.apache.commons.lang3.StringUtils;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Controller for _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 */
@Controller
public class ArticleCrudController extends RestController {

  private static final Logger log = LogManager.getLogger(ArticleCrudController.class);

  private static final String FROM_DATE = "fromDate";

  private static final String TO_DATE = "toDate";

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private ArticleRevisionWriteService articleRevisionWriteService;
  @Autowired
  private CommentCrudService commentCrudService;
  @Autowired
  private AssetFileCrudController assetFileCrudController;
  @Autowired
  private ArticleListCrudService articleListCrudService;
  @Autowired
  private TaxonomyService taxonomyService;
  @Autowired
  private RelationshipViewFactory relationshipViewFactory;

  /**
   * Calculate the date range using the specified rule. For example:
   *
   * <ul>
   * <li>sinceRule=2y  - 2 years</li>
   * <li>sinceRule=5m  - 5 months</li>
   * <li>sinceRule=10d - 10 days</li>
   * <li>sinceRule=5h  - 5 hours</li>
   * <li>sinceRule=33  - 33 minutes</li>
   * </ul>
   *
   * The method will result in a {@link java.util.Map Map} containing the following keys:
   *
   * <ul>
   * <li><b>fromDate</b> - the starting date
   * <li><b>toDate</b> - the ending date, which will be the current system date (i.e. now())
   * </ul>
   *
   * @param sinceRule The rule to calculate the date range
   *
   * @return A {@link java.util.Map Map}
   */
  public static final Map<String, LocalDateTime> calculateDateRange(String sinceRule) {
    if (StringUtils.isBlank(sinceRule)) {
      return ImmutableMap.of();
    }

    final String timeDesignation = StringUtils.right(sinceRule, 1);
    long timeDelta = 0;
    try {
      // Assume last character is NOT a letter (i.e. all characters are digits).
      timeDelta = Long.parseLong(sinceRule);
    } catch (NumberFormatException exception) {
      // If an exception, then last character MUST have been a letter,
      // so we now exclude the last character and re-try conversion.
      try {
        timeDelta = Long.parseLong(sinceRule.substring(0, sinceRule.length() - 1));
      } catch (NumberFormatException error) {
        log.warn("Failed to convert {} to a timeDelta/timeDesignation!", sinceRule);
        timeDelta = 0;
      }
    }

    if (timeDelta < 1) {
      return ImmutableMap.of();
    }

    final LocalDateTime toDate = LocalDateTime.now();
    final LocalDateTime fromDate;
    if (timeDesignation.equalsIgnoreCase("y")) {
      fromDate = toDate.minusYears(timeDelta);
    } else if (timeDesignation.equalsIgnoreCase("m")) {
      fromDate = toDate.minusMonths(timeDelta);
    } else if (timeDesignation.equalsIgnoreCase("d")) {
      fromDate = toDate.minusDays(timeDelta);
    } else if (timeDesignation.equalsIgnoreCase("h")) {
      fromDate = toDate.minus(timeDelta, ChronoUnit.HOURS);
    } else {
      fromDate = toDate.minus(timeDelta, ChronoUnit.MINUTES);
    }

    final ImmutableMap<String, LocalDateTime> dateRange = ImmutableMap.of(
        FROM_DATE, fromDate, TO_DATE, toDate);
    return dateRange;
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/articles/page/{pageNumber}", method = RequestMethod.GET)
  public ResponseEntity<?> listDois(
      @PathVariable(value="pageNumber") int pageNumber,
      @RequestParam(value="pageSize", required=false, defaultValue="100") int pageSize,
      @RequestParam(value="orderBy", required=false, defaultValue="newest") String orderBy,
      @RequestParam(value="since", required=false, defaultValue="") String sinceRule)
          throws IOException {
    final ArticleCrudService.SortOrder sortOrder = ArticleCrudService.SortOrder.valueOf(
        StringUtils.upperCase(StringUtils.defaultString(orderBy, "newest" /* defaultStr */)));

    final Map<String, LocalDateTime>dateRange = calculateDateRange(sinceRule);
    final Optional<LocalDateTime> fromDate = Optional.ofNullable(dateRange.getOrDefault(
        FROM_DATE, null));
    final Optional<LocalDateTime> toDate = Optional.ofNullable(dateRange.getOrDefault(
        TO_DATE, null));
    final Collection<String> articleDois = articleCrudService.getArticleDoisForDateRange(
        pageNumber, pageSize, sortOrder, fromDate, toDate);
    return ServiceResponse.serveView(articleDois).asJsonResponse(entityGson);
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
                                             @RequestParam("preprintDoi") String preprintDoi)
      throws IOException {
    ArticleIngestionIdentifier ingestionId =
        ArticleIngestionIdentifier.create(DoiEscaping.unescape(doi), ingestionNumber);

    articleCrudService.updatePreprintDoi(ingestionId, preprintDoi);

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
    return ServiceResponse.serveView(relationshipViewFactory.getRelationshipViews(id)).asJsonResponse(entityGson);
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
                                              @RequestParam(value = "toDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate
                                              ) throws IOException {
    List<ArticleRevisionView> views = articleCrudService.getArticlesPublishedOn(fromDate, toDate)
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
                                            @RequestParam(value = "toDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate
                                            ) throws IOException {
    List<ArticleRevisionView> views = articleCrudService.getArticlesRevisedOn(fromDate, toDate)
        .stream().map(ArticleRevisionView::getView)
        .collect(Collectors.toList());
    return ServiceResponse.serveView(views).asJsonResponse(entityGson);
  }
}
