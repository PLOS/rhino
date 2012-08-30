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

package org.ambraproject.admin.service;

import org.ambraproject.filestore.FileStoreException;

import java.io.IOException;
import java.io.InputStream;

public interface ArticleCrudService {

  /**
   * Create an article from supplied XML data.
   * <p/>
   * The input stream is closed after being successfully read, but this is not guaranteed. Any invocation of this method
   * must be enclosed in a {@code try} block, with the argument input stream closed in the {@code finally} block.
   *
   * @param file the XML data for the created article
   * @param doi  the DOI of the created article
   * @throws org.ambraproject.admin.RestClientException
   *                            if the DOI is already used
   * @throws IOException
   * @throws FileStoreException
   */
  public abstract void create(InputStream file, String doi) throws IOException, FileStoreException;

  /**
   * Open a stream to read the XML file for an article, as raw bytes. The caller must close the stream.
   *
   * @param doi the DOI of the article
   * @return a stream containing the XML file
   * @throws org.ambraproject.admin.RestClientException
   *                            if the DOI does not belong to an article
   * @throws FileStoreException
   */
  public abstract InputStream read(String doi) throws FileStoreException;

  /**
   * Overwrite an article with supplied XML data.
   * <p/>
   * The input stream is closed after being successfully read, but this is not guaranteed. Any invocation of this method
   * must be enclosed in a {@code try} block, with the argument input stream closed in the {@code finally} block.
   *
   * @param file the XML data for the article
   * @param doi  the DOI of the article
   * @throws org.ambraproject.admin.RestClientException
   *                            if the DOI does not belong to an article
   * @throws IOException
   * @throws FileStoreException
   */
  public abstract void update(InputStream file, String doi) throws IOException, FileStoreException;

  /**
   * Delete an article. Both its database entry and the associated XML file in the file store are deleted.
   *
   * @param doi the DOI of the article to delete
   * @throws org.ambraproject.admin.RestClientException
   *                            if the DOI does not belong to an article
   * @throws FileStoreException
   */
  public abstract void delete(String doi) throws FileStoreException;

}
