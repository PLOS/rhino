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

import com.google.common.base.Optional;
import org.ambraproject.admin.controller.MetadataFormat;
import org.ambraproject.admin.identity.DoiBasedIdentity;
import org.ambraproject.filestore.FileStoreException;

import java.io.IOException;
import java.io.InputStream;

public interface AssetCrudService extends DoiBasedCrudService {

  /**
   * Create or update an article asset.
   *
   * @param file      the file data to associate with the new asset
   * @param assetId   the identifier for the new asset
   * @param articleId the identifier for the existing article to which the asset belongs
   * @return an indication of whether the asset was created or updated
   * @throws FileStoreException
   * @throws IOException
   */
  public abstract WriteResult upload(InputStream file,
                                     DoiBasedIdentity assetId,
                                     Optional<DoiBasedIdentity> articleId)
      throws FileStoreException, IOException;

  /**
   * Read the file associated with an asset.
   *
   * @param id the identifier of the asset whose file is to be read
   * @return a stream containing the file data
   * @throws FileStoreException
   */
  public abstract InputStream read(DoiBasedIdentity id) throws FileStoreException;

  /**
   * Delete an asset and its associated file.
   *
   * @param id the identifier of the asset to delete
   * @throws FileStoreException
   */
  public abstract void delete(DoiBasedIdentity id) throws FileStoreException;

  /**
   * Read the metadata of an asset.
   *
   * @param id     the identifier of the asset
   * @param format the desired metadata format
   * @return the metadata
   * @throws org.ambraproject.admin.RestClientException
   *          if the DOI does not belong to an article
   */
  public abstract String readMetadata(DoiBasedIdentity id, MetadataFormat format);
}
