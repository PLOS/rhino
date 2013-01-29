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
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

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
   * @return an indication of which article was created or updated
   * @throws org.ambraproject.rhino.rest.RestClientException
   *                            if the DOI is already used
   * @throws IOException
   * @throws FileStoreException
   */
  public abstract WriteResult<Article> write(InputStream file, Optional<ArticleIdentity> suppliedId, WriteMode mode)
      throws IOException, FileStoreException;

  /**
   * Open a stream to read the XML file for an article, as raw bytes. The caller must close the stream.
   *
   * @param id the identifier of the article
   * @return a stream containing the XML file
   * @throws org.ambraproject.rhino.rest.RestClientException
   *                            if the DOI does not belong to an article
   * @throws FileStoreException
   */
  public abstract InputStream readXml(ArticleIdentity id) throws FileStoreException;

  /**
   * Delete an article. Both its database entry and the associated XML file in the file store are deleted.
   *
   * @param id the identifier of the article to delete
   * @throws org.ambraproject.rhino.rest.RestClientException
   *                            if the DOI does not belong to an article
   * @throws FileStoreException
   */
  public abstract void delete(ArticleIdentity id) throws FileStoreException;

  /**
   * Read the metadata of an article.
   *
   * @param response the response object to which to send the metadata
   * @param id       the identifier of the article
   * @param format   the desired metadata format
   * @throws org.ambraproject.rhino.rest.RestClientException
   *          if the DOI does not belong to an article
   */
  public abstract void readMetadata(HttpServletResponse response, DoiBasedIdentity id, MetadataFormat format)
      throws IOException;

  /**
   * Read the metadata of an article.
   *
   * @param response the response object to which to send the metadata
   * @param article  the article
   * @param format   the desired metadata format
   * @throws org.ambraproject.rhino.rest.RestClientException
   *          if the DOI does not belong to an article
   */
  public abstract void readMetadata(HttpServletResponse response, Article article, MetadataFormat format)
      throws IOException;

}
