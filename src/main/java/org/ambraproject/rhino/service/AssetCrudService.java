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

import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.util.response.Transceiver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public interface AssetCrudService extends DoiBasedCrudService {

  /**
   * Upload a file to be associated with a created asset.
   *
   * @param file    the file data to associate with the new asset
   * @param assetId the identifier for the existing asset with the new file's extension
   * @return an indication of the result
   * @throws FileStoreException
   * @throws IOException
   */
  public abstract WriteResult<ArticleAsset> upload(InputStream file,
                                                   AssetFileIdentity assetId)
      throws FileStoreException, IOException;

  /**
   * Read the file associated with an asset.
   *
   * @param id the identifier of the asset whose file is to be read
   * @return a stream containing the file data
   */
  public abstract InputStream read(AssetFileIdentity id);

  /**
   * Return the data needed to build reproxying headers for an asset.
   *
   * @param assetId the identifier of the asset being proxied
   * @return a list of reproxy URLs
   * @throws IOException
   */
  public abstract List<URL> reproxy(AssetFileIdentity assetId) throws IOException;

  /**
   * Delete an asset and its associated file.
   *
   * @param id the identifier of the asset to delete
   * @throws FileStoreException
   */
  public abstract void delete(AssetFileIdentity id) throws FileStoreException;

  /**
   * Read the metadata of an asset. The output may contain multiple asset objects, one for each file associated with the
   * asset.
   *
   * @param id the identity of the asset to read
   */
  public abstract Transceiver readMetadata(AssetIdentity id)
      throws IOException;

  /**
   * Read the metadata of a figure asset. The output contains the figure metadata, as defined by the "original" asset
   * file, plus the individual asset file objects.
   *
   * @param id the identity of the asset to read
   */
  public abstract Transceiver readFigureMetadata(AssetIdentity id)
      throws IOException;

  /**
   * Read the metadata of a single asset file.
   *
   * @param id the identity of the asset file to read
   */
  public Transceiver readFileMetadata(AssetFileIdentity id)
      throws IOException;

  /**
   * Overwrite an existing asset's file with a new file.
   *
   * @param fileContent the file to write
   * @param id          the identity of the asset
   */
  public abstract void overwrite(InputStream fileContent, AssetFileIdentity id) throws IOException, FileStoreException;

  /**
   * Return the identity of the article to which an asset belongs.
   *
   * @param id the asset's identity
   * @return the article's identity
   */
  public abstract ArticleIdentity findArticleFor(AssetIdentity id);
}
