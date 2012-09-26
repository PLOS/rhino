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

import org.ambraproject.admin.controller.DoiBasedIdentity;
import org.ambraproject.filestore.FileStoreException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

public interface ArticleCrudService extends DoiBasedCrudService {

  /**
   * Create or update an article from supplied XML data. If no article exists with the given identity, a new article
   * entity is created; else, the article is re-ingested and the new data replaces the old data in the file store.
   * <p/>
   * The input stream is closed after being successfully read, but this is not guaranteed. Any invocation of this method
   * must be enclosed in a {@code try} block, with the argument input stream closed in the {@code finally} block.
   *
   * @param file the XML data for the article
   * @param id   the identifier for the article
   * @return an indication of whether the article was created or updated
   * @throws org.ambraproject.admin.RestClientException
   *                            if the DOI is already used
   * @throws IOException
   * @throws FileStoreException
   */
  public abstract AmbraService.UploadResult upload(InputStream file, DoiBasedIdentity id)
      throws IOException, FileStoreException;

  /**
   * Open a stream to read the XML file for an article, as raw bytes. The caller must close the stream.
   *
   * @param id the identifier of the article
   * @return a stream containing the XML file
   * @throws org.ambraproject.admin.RestClientException
   *                            if the DOI does not belong to an article
   * @throws FileStoreException
   */
  public abstract InputStream read(DoiBasedIdentity id) throws FileStoreException;

  /**
   * Delete an article. Both its database entry and the associated XML file in the file store are deleted.
   *
   * @param id the identifier of the article to delete
   * @throws org.ambraproject.admin.RestClientException
   *                            if the DOI does not belong to an article
   * @throws FileStoreException
   */
  public abstract void delete(DoiBasedIdentity id) throws FileStoreException;

  /**
   * Open a reader to read the article metadata as human-friendly JSON text.
   *
   * @param id the identifier of the article
   * @return a reader containing the metadata as JSON
   * @throws org.ambraproject.admin.RestClientException
   *          if the DOI does not belong to an article
   */
  public abstract Reader readMetadata(DoiBasedIdentity id);

}
