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
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;

import java.io.IOException;
import java.io.InputStream;

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
   * Delete an asset and its associated file.
   *
   * @param id the identifier of the asset to delete
   * @throws FileStoreException
   */
  public abstract void delete(AssetFileIdentity id) throws FileStoreException;

  /**
   * Read the metadata of an asset.
   *
   * @param id     the identifier of the asset
   * @param format the desired metadata format
   * @return the metadata
   * @throws org.ambraproject.rhino.rest.RestClientException
   *          if the DOI does not belong to an article
   */
  public abstract String readMetadata(DoiBasedIdentity id, MetadataFormat format);

}
