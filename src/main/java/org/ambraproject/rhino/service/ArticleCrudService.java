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
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.response.CacheableServiceResponse;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.rhino.view.ResolvedDoiView;
import org.ambraproject.rhino.view.article.ArticleOverview;
import org.w3c.dom.Document;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ArticleCrudService {

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
  public abstract CacheableServiceResponse serveAuthors(ArticleIngestionIdentifier ingestionId);

  /**
   * Read category information from the Ambra database.
   *
   * @param articleId specifies the article
   * @throws IOException
   */
  public abstract ServiceResponse serveCategories(ArticleIdentifier articleId)
      throws IOException;

  /**
   * Get raw taxonomy terms from the taxonomy server about an article.
   *
   * @param articleId specifies the article
   * @throws IOException
   */
  public abstract ServiceResponse serveRawCategories(ArticleIdentifier articleId)
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

  public abstract CacheableServiceResponse serveMetadata(ArticleIngestionIdentifier ingestionId);

  public abstract CacheableServiceResponse serveItems(ArticleIngestionIdentifier ingestionId);

  public abstract ArticleOverview buildOverview(Article article);

  public abstract ServiceResponse serveOverview(ArticleIdentifier id);

  public abstract ServiceResponse serveRevisions(ArticleIdentifier id);

  public abstract CacheableServiceResponse serveRevision(ArticleRevisionIdentifier revisionId);

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

  /**
   * Get all the article revisions published within a given date range
   */
  public abstract Collection<ArticleRevision> getArticlesPublishedOn(LocalDate fromDate, LocalDate toDate);

  /**
   * Get all the article revisions revised within a given date range
   */
  public abstract Collection<ArticleRevision> getArticlesRevisedOn(LocalDate fromDate, LocalDate toDate);


}
