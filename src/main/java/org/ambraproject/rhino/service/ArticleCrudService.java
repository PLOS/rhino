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
import org.ambraproject.rhino.identity.ArticleIdentity;
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
   * Create or update an article from supplied XML data. If no article exists with the given identity, a new article
   * entity is created; else, the article is re-ingested and the new data replaces the old data in the file store.
   * <p/>
   * The input stream is closed after being successfully read, but this is not guaranteed. Any invocation of this method
   * must be enclosed in a {@code try} block, with the argument input stream closed in the {@code finally} block.
   *
   * @param file       the XML data for the article
   * @param suppliedId the identifier supplied for the article, if any
   * @return the created or update Article
   * @throws org.ambraproject.rhino.rest.RestClientException if the DOI is already used
   * @throws IOException
   */
  public abstract Article write(InputStream file, Optional<ArticleIdentity> suppliedId, WriteMode mode)
      throws IOException;

  /**
   * Create or update an article from supplied ,zip archive data. If no article exists with the given identity, a new
   * article entity is created; else, the article is re-ingested and the new data replaces the old data in the file
   * store.
   *
   * @param filename   path to the local .zip file
   * @param suppliedId the identifier supplied for the article, if any
   * @return the created or update Article
   * @throws org.ambraproject.rhino.rest.RestClientException if the DOI is already used
   * @throws IOException
   */
  public abstract Article writeArchive(Archive archive, Optional<ArticleIdentity> suppliedId, WriteMode mode)
      throws IOException;

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
  public abstract Article findArticleById(ArticleIdentity id);

  /**
   * Read the metadata of an article.
   *
   * @param id               the identifier of the article
   * @throws org.ambraproject.rhino.rest.RestClientException if the DOI does not belong to an article
   */
  public abstract Transceiver readMetadata(ArticleIdentity id) throws IOException;

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

}
