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

import org.ambraproject.rhino.identity.ArticleFileIdentifier;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.util.response.Transceiver;
import org.plos.crepo.model.metadata.RepoObjectMetadata;

import java.io.IOException;
import java.io.InputStream;

public interface AssetCrudService {

  /**
   * Read the file associated with an asset.
   *
   * @param id the identifier of the asset whose file is to be read
   * @return a stream containing the file data
   */
  public abstract InputStream read(AssetFileIdentity id);

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

  public abstract RepoObjectMetadata getArticleItemFile(ArticleFileIdentifier fileId);

}
