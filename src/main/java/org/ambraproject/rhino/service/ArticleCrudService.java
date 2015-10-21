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

import com.google.common.base.Optional;
import org.ambraproject.models.Article;
import org.ambraproject.models.Journal;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.impl.RecentArticleQuery;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.article.ArticleCriteria;
import org.ambraproject.rhino.view.article.RelatedArticleView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public interface ArticleCrudService extends DoiBasedCrudService {

  /**
   * Create or update an article from supplied ,zip archive data. If no article exists with the given identity, a new
   * article entity is created; else, the article is re-ingested and the new data replaces the old data in the file
   * store.
   *
   * @param archive    the local .zip file
   * @param suppliedId the identifier supplied for the article, if any
   * @return the created or update Article
   * @throws org.ambraproject.rhino.rest.RestClientException if the DOI is already used
   * @throws IOException
   */
  public abstract Article writeArchive(Archive archive, Optional<ArticleIdentity> suppliedId, WriteMode mode)
      throws IOException;

  /**
   * Ingest an article under the versioned data model only. This should be used only for articles that were ingested
   * normally, prior to the update to {@link #writeArchive} that made
   *
   * @param archive
   * @return
   * @throws IOException
   * @deprecated This is a temporary kludge for data migration. It should be deleted when the legacy article model is no
   * longer supported.
   */
  @Deprecated
  public abstract Article writeArchiveAsVersionedOnly(Archive archive) throws IOException;

  /**
   * Repopulates article category information by making a call to the taxonomy server.
   *
   * @param id the identifier of the article
   */
  public abstract void repopulateCategories(ArticleIdentity id) throws IOException;

  /**
   * Open a stream to read the XML file for an article, as raw bytes. The caller must close the stream.
   *
   * @param id the identifier of the article
   * @return a stream containing the XML file
   * @throws org.ambraproject.rhino.rest.RestClientException if the DOI does not belong to an article
   */
  public abstract InputStream readXml(ArticleIdentity id);

  /**
   * Delete an article. Both its database entry and the associated XML file in the file store are deleted.
   *
   * @param id the identifier of the article to delete
   * @throws org.ambraproject.rhino.rest.RestClientException if the DOI does not belong to an article
   */
  public abstract void delete(ArticleIdentity id);

  /**
   * Loads and returns article metadata.
   *
   * @param id specifies the article
   * @return Article object encapsulating metadata
   */
  public abstract Article findArticleById(DoiBasedIdentity id);

  /**
   * Retrieve an article's publication {@code journal} field based on the article's {@code eIssn}
   * field. Always expects {@code eIssn} to match to a journal in the system.
   *
   * @param article the article to modify
   * @throws RestClientException if {@code article.eIssn} is null or the {@code article.eIssn}
   *                         isn't matched to a journal in the database
   */

  public abstract Journal getPublicationJournal(Article article) throws RestClientException;

  /**
   * Read the metadata of an article.
   *
   * @param id               the identifier of the article
   * @param excludeCitations if true, no citation information will be included in the response (useful for performance
   *                         reasons, since this is a lot of data)
   * @throws org.ambraproject.rhino.rest.RestClientException if the DOI does not belong to an article
   */
  public abstract Transceiver readMetadata(DoiBasedIdentity id, boolean excludeCitations) throws IOException;

  /**
   * Read the metadata of an article.
   *
   * @param article          the article
   * @param excludeCitations if true, no citation information will be included in the response (useful for performance
   *                         reasons, since this is a lot of data)
   * @throws org.ambraproject.rhino.rest.RestClientException if the DOI does not belong to an article
   */
  public abstract Transceiver readMetadata(Article article, boolean excludeCitations) throws IOException;

  /**
   * Read information about the authors of an article.
   *
   * @param id specifies the article
   * @throws IOException
   */
  public abstract Transceiver readAuthors(ArticleIdentity id)
      throws IOException;

  /**
   * Read category information from the Ambra database.
   *
   * @param id specifies the article
   * @throws IOException
   */
  public abstract Transceiver readCategories(ArticleIdentity id)
      throws IOException;

  /**
   * Get raw taxonomy terms from the taxonomy server about an article.
   *
   * @param id specifies the article
   * @throws IOException
   */
  public abstract Transceiver getRawCategories(ArticleIdentity id)
      throws IOException;

  /**
   * Get the text that is sent to the taxonomy server as well as the taxonomy terms returned by the server
   *
   * @param id specifies the article
   * @return a String containing the text and raw categories
   * @throws IOException
   */
  public abstract String getRawCategoriesAndText(ArticleIdentity id) throws IOException;

  public abstract void setAssetService(AssetCrudService assetService);

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
  public abstract Collection<RelatedArticleView> getRelatedArticles(Article article);

  /**
   * Read the metadata of a random article.
   *
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


  /**
   * Replicates the behavior of {@link #readMetadata}, and forces the service to read from the versioned data model.
   *
   * @deprecated <em>TEMPORARY.</em> To be removed when the versioned data model is fully supported.
   */
  @Deprecated
  public abstract Transceiver readVersionedMetadata(ArticleIdentity id,
                                                    Optional<Integer> versionNumber,
                                                    ArticleMetadataSource source);

  /**
   * Signifies which file to use when reading article metadata from a content repo collection. This exists for
   * verification purposes only; when the versioned data model is stable, the service will use only one data source as
   * an implementation choice.
   *
   * @deprecated <em>TEMPORARY.</em> To be removed when regular services fully use the versioned data model.
   */
  @Deprecated
  public static enum ArticleMetadataSource {
    FULL_MANUSCRIPT, FRONT_MATTER, FRONT_AND_BACK_MATTER
  }

}
