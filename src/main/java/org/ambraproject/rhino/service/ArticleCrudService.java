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

package org.ambraproject.rhino.service;

import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.ArticleItemIdentifier;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ArticleRelationship;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.article.RelatedArticleLink;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.response.CacheableResponse;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.rhino.view.ResolvedDoiView;
import org.ambraproject.rhino.view.article.ArticleIngestionView;
import org.ambraproject.rhino.view.article.ArticleOverview;
import org.ambraproject.rhino.view.article.ArticleRevisionView;
import org.ambraproject.rhino.view.article.CategoryAssignmentView;
import org.ambraproject.rhino.view.article.ItemSetView;
import org.ambraproject.rhino.view.article.author.ArticleAllAuthorsView;
import org.plos.crepo.model.metadata.RepoObjectMetadata;
import org.w3c.dom.Document;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ArticleCrudService {

  /** Sort ordering types. */
  public enum SortOrder {
    OLDEST, NEWEST
  }

  /**
   * Populates article category information by making a call to the taxonomy server.
   *
   * @param articleId the identifier of the article
   */
  public abstract void populateCategories(ArticleIdentifier articleId) throws IOException;

  /**
   * Read information about the authors of an article.
   *
   * @param ingestionId specifies the article
   * @throws IOException
   */
  public abstract CacheableResponse<ArticleAllAuthorsView> serveAuthors(ArticleIngestionIdentifier ingestionId);

  /**
   * Read category information from the Ambra database.
   *
   * @param articleId specifies the article
   * @throws IOException
   */
  public abstract ServiceResponse<Collection<CategoryAssignmentView>> serveCategories(ArticleIdentifier articleId)
      throws IOException;

  /**
   * Get raw taxonomy terms from the taxonomy server about an article.
   *
   * @param articleId specifies the article
   * @throws IOException
   */
  public abstract ServiceResponse<List<String>> serveRawCategories(ArticleIdentifier articleId)
      throws IOException;

  /**
   * Get the text that is sent to the taxonomy server as well as the taxonomy terms returned by the server
   *
   * @param articleId specifies the article
   * @return a String containing the text and raw categories
   * @throws IOException
   */
  public abstract String getRawCategoriesAndText(ArticleIdentifier articleId) throws IOException;

  List<ArticleRelationship> getRelationshipsFrom(ArticleIdentifier sourceId);

  List<ArticleRelationship> getRelationshipsTo(ArticleIdentifier targetId);

  void refreshArticleRelationships(ArticleRevision sourceArticleRev);

  /**
   * Recreate an ingested archive.
   * <p>
   * The recreated archive will contain the same file entries with the same names, but is not guaranteed to be a
   * byte-for-byte copy of the original zip file. It may vary in details such as entry order, compression parameters,
   * file timestamps, and zip comments.
   *
   * @param ingestionId the ingestion created by ingesting the original archive
   * @return a copy of the original archive
   */
  public abstract Archive repack(ArticleIngestionIdentifier ingestionId);

  public abstract CacheableResponse<ArticleIngestionView> serveMetadata(ArticleIngestionIdentifier ingestionId);

  public abstract CacheableResponse<ItemSetView> serveItems(ArticleIngestionIdentifier ingestionId);

  public abstract ArticleOverview buildOverview(Article article);

  public abstract ServiceResponse<ArticleOverview> serveOverview(ArticleIdentifier id);

  public abstract ServiceResponse<List<ArticleRevisionView>> serveRevisions(ArticleIdentifier id);

  public abstract CacheableResponse<ArticleRevisionView> serveRevision(ArticleRevisionIdentifier revisionId);

  public abstract ArticleItem getArticleItem(ArticleItemIdentifier id);

  public abstract Collection<ArticleItem> getAllArticleItems(Doi doi);

  public abstract Collection<ArticleItem> getAllArticleItems(ArticleIngestion ingestion);

  Optional<ResolvedDoiView> getItemOverview(Doi doi);

  /**
   * Get an ingestion if it exists.
   */
  public abstract Optional<ArticleIngestion> getIngestion(ArticleIngestionIdentifier ingestionId);

  /**
   * Read an ingestion requested by the client, throwing {@link RestClientException} if it is not found.
   */
  public abstract ArticleIngestion readIngestion(ArticleIngestionIdentifier ingestionId);

  /**
   * Get a revision if it exists.
   */
  public abstract Optional<ArticleRevision> getRevision(ArticleRevisionIdentifier revisionId);

  /**
   * Read a revision requested by the client, throwing {@link RestClientException} if it is not found.
   */
  public abstract ArticleRevision readRevision(ArticleRevisionIdentifier revisionId);

  /**
   * Get an article's latest revision, if it has any revisions.
   */
  public abstract Optional<ArticleRevision> getLatestRevision(Article article);

  /**
   * Get the latest revision of an article requested by the client, throwing {@link RestClientException} if the article
   * has no revisions.
   */
  public abstract ArticleRevision readLatestRevision(Article article);

  /**
   * Get an article if it exists.
   */
  public abstract Optional<Article> getArticle(ArticleIdentifier articleIdentifier);

  /**
   * Read an article requested by the client, throwing {@link RestClientException} if it is not found.
   */
  public abstract Article readArticle(ArticleIdentifier articleIdentifier);

  public abstract Document getManuscriptXml(ArticleIngestion articleIngestion);

  public abstract Document getManuscriptXml(RepoObjectMetadata objectMetadata);

  public abstract RepoObjectMetadata getManuscriptMetadata(ArticleIngestion ingestion);

  /**
   * Get all the article revisions published within a given date range
   */
  public abstract Collection<ArticleRevision> getArticlesPublishedOn(LocalDate fromDate, LocalDate toDate);

  /**
   * Get all the article revisions revised within a given date range
   */
  public abstract Collection<ArticleRevision> getArticlesRevisedOn(LocalDate fromDate, LocalDate toDate);

  /**
   * Sets the "preprintDoi" field on the article ingestion.
   *
   * @param articleId     the identifier of the article
   * @param preprintOfDoi doi of the VOR for this preprint
   */
  public abstract void updatePreprintDoi(ArticleIngestionIdentifier articleId, String preprintOfDoi) throws IOException;

  /**
   * Get the article DOIs.
   *
   * Method will return a <b>paginated</b> list of the DOIs.
   *
   * @param pageNumber The page number to retrieve
   * @param pageSize The number of results to retrieve
   * @param sortOrder The order by method (i.e. order by oldest or newest)
   *
   * @return The list for DOIs
   */
  public abstract Collection<String> getArticleDois(
      int pageNumber, int pageSize, SortOrder sortOrder);

  /**
   * Get the article DOIs, for a given <b>date range</b>.
   *
   * Method will return a <b>paginated</b> list of the DOIs.
   *
   * @param pageNumber The page number to retrieve
   * @param pageSize The number of results to retrieve
   * @param sortOrder The order by method (i.e. order by oldest or newest)
   * @param fromDate The starting date range
   * @param toDate The ending date range
   *
   * @return The list for DOIs
   */
  public abstract Collection<String> getArticleDoisForDateRange(
      int pageNumber, int pageSize, SortOrder sortOrder, Optional<LocalDateTime> fromDate,
      Optional<LocalDateTime> toDate);

  /**
   * Build an ArticleRelationship.
   *
   * @param Article source article
   * @param RelatedArticleLink Data parsed from XML
   */
  public abstract ArticleRelationship fromRelatedArticleLink(Article article, RelatedArticleLink ral);
}
