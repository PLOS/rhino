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

public interface AssetCrudService extends ArticleSpaceCrudService {

  /**
   * Create a new article asset.
   * <p/>
   * This temporary API is based on the assumption that there is exactly one file associated with each asset. This
   * should be refactored when the role of assets is generalized later.
   *
   * @param file the file data to associate with the new asset
   * @param id   the identifier for the new asset
   * @throws FileStoreException
   * @throws IOException
   */
  public abstract void create(InputStream file, DoiBasedIdentity id) throws FileStoreException, IOException;

  /**
   * Read the file associated with an asset.
   *
   * @param id the identifier of the asset whose file is to be read
   * @return a stream containing the file data
   * @throws FileStoreException
   */
  public abstract InputStream read(DoiBasedIdentity id) throws FileStoreException;

  /**
   * Replace asset data from a provided file stream.
   * <p/>
   * This temporary API is based on the assumption that there is exactly one file associated with each asset. This
   * should be refactored when the role of assets is generalized later.
   *
   * @param file a stream containing the data to write
   * @param id   the identifier of the asset to update
   * @throws FileStoreException
   * @throws IOException
   */
  public abstract void update(InputStream file, DoiBasedIdentity id) throws FileStoreException, IOException;

  /**
   * Delete an asset and its associated file.
   *
   * @param id the identifier of the asset to delete
   * @throws FileStoreException
   */
  public abstract void delete(DoiBasedIdentity id) throws FileStoreException;

}
