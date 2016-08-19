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
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.ArticleItemIdentifier;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.VersionedArticleRelationship;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.impl.RecentArticleQuery;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.ResolvedDoiView;
import org.ambraproject.rhino.view.article.ArticleCriteria;
import org.ambraproject.rhino.view.article.RelatedArticleView;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
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
   * Open a stream to read the XML file for an article, as raw bytes. The caller must close the stream.
   *
   * @param id the identifier of the article
   * @return a stream containing the XML file
   * @throws org.ambraproject.rhino.rest.RestClientException if the DOI does not belong to an article
   */
  public abstract InputStream readXml(ArticleIdentity id);

  /**
   * Read the metadata of an article.
   *
   * @param id               the identifier of the article
   * @param excludeCitations if true, no citation information will be included in the response (useful for performance
   *                         reasons, since this is a lot of data)
   * @throws org.ambraproject.rhino.rest.RestClientException if the DOI does not belong to an article
   */
  public abstract Transceiver serveMetadata(DoiBasedIdentity id, boolean excludeCitations) throws IOException;

  /**
   * Read the metadata of an article.
   *
   * @param article          the article
   * @param excludeCitations if true, no citation information will be included in the response (useful for performance
   *                         reasons, since this is a lot of data)
   * @throws org.ambraproject.rhino.rest.RestClientException if the DOI does not belong to an article
   */
  public abstract Transceiver serveMetadata(Article article, boolean excludeCitations) throws IOException;

  /**
   * Read information about the authors of an article.
   *
   * @param ingestionId specifies the article
   * @throws IOException
   */

  public abstract Transceiver serveAuthors(ArticleIngestionIdentifier ingestionId);

  /**
   * Read category information from the Ambra database.
   *
   * @param articleId specifies the article
   * @throws IOException
   */
  public abstract Transceiver serveCategories(ArticleIdentifier articleId)
      throws IOException;

  /**
   * Get raw taxonomy terms from the taxonomy server about an article.
   *
   * @param articleId specifies the article
   * @throws IOException
   */
  public abstract Transceiver serveRawCategories(ArticleIdentifier articleId)
      throws IOException;

  /**
   * Get the text that is sent to the taxonomy server as well as the taxonomy terms returned by the server
   *
   * @param articleId specifies the article
   * @return a String containing the text and raw categories
   * @throws IOException
   */
  public abstract String getRawCategoriesAndText(ArticleIdentifier articleId) throws IOException;

  /**
   * List the DOIs of all ingested articles, or a described subset.
   *
   * @param articleCriteria description of the subset of articles to list
   */
  public abstract Transceiver listDois(ArticleCriteria articleCriteria)
      throws IOException;

  /**
   * Carry out a query for recent articles.
   *
   * @see org.ambraproject.rhino.service.impl.RecentArticleQuery
   */
  public abstract Transceiver listRecent(RecentArticleQuery query)
      throws IOException;

  /**
   * Produce views of an article's related articles. Wraps the objects returned by {@link Article#getRelatedArticles()}
   * and adds those articles' titles and author lists.
   *
   * @param article an article with a populated {@code relatedArticles} field
   * @return a set of views of the related articles
   */
  public abstract List<RelatedArticleView> getRelatedArticles(Article article);

  List<VersionedArticleRelationship> getRelationshipsFrom(ArticleIdentifier sourceId);

  List<VersionedArticleRelationship> getRelationshipsTo(ArticleIdentifier targetId);

  void refreshArticleRelationships(ArticleRevision sourceArticleRev);

  /**
   * Read the metadata of a random article.
   * <p>
   * <em>WARNING</em> random retrieval of records is not performant and should be used only for testing
   */
  public abstract Transceiver readRandom() throws IOException;

  /**
   * Represent an existing article as an ingestible archive. The archive, if it were reingested, should produce an
   * identical article and assets.
   *
   * @param articleIdentity the article to represent
   * @return the archive
   */
  public abstract Archive repack(ArticleIdentity articleIdentity);

  public abstract Transceiver serveMetadata(ArticleIngestionIdentifier ingestionId);

  public abstract Transceiver serveItems(ArticleIngestionIdentifier ingestionId);

  public abstract Transceiver serveOverview(ArticleIdentifier id);

  Transceiver serveRevision(ArticleRevisionIdentifier revisionId);

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
  public abstract Optional<ArticleRevision> getLatestRevision(ArticleTable article);

  /**
   * Get the latest revision of an article requested by the client, throwing {@link RestClientException} if the article
   * has no revisions.
   */
  public abstract ArticleRevision readLatestRevision(ArticleTable article);

  /**
   * Get an article if it exists.
   */
  public abstract Optional<ArticleTable> getArticle(ArticleIdentifier articleIdentifier);

  /**
   * Read an article requested by the client, throwing {@link RestClientException} if it is not found.
   */
  public abstract ArticleTable readArticle(ArticleIdentifier articleIdentifier);

  public abstract Document getManuscriptXml(ArticleIngestion articleIngestion);

}
