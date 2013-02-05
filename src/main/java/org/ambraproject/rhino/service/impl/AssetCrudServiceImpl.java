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

package org.ambraproject.rhino.service.impl;

import com.google.inject.internal.Preconditions;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.WriteResult;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class AssetCrudServiceImpl extends AmbraService implements AssetCrudService {

  private boolean assetExistsAt(DoiBasedIdentity id) {
    DetachedCriteria criteria = DetachedCriteria.forClass(ArticleAsset.class)
        .add(Restrictions.eq("doi", id.getKey()));
    return exists(criteria);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WriteResult<ArticleAsset> upload(InputStream file, AssetFileIdentity assetFileId)
      throws FileStoreException, IOException {
    // TODO Improve efficiency; loading and persisting the whole Article object is too slow
    Article article = (Article) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.find(
            // Find the article that contains an asset with the given DOI
            "select article from Article article join article.assets asset where asset.doi = ?",
            assetFileId.getKey()));
    List<ArticleAsset> assets = article.getAssets();

    /*
     * Set up the Hibernate entity that we want to modify. If this is the first file to be uploaded, then there is a
     * file-less asset that we need to modify. Else, create a new asset, but copy its article-defined fields over from
     * an existing asset.
     *
     * TODO Shouldn't need the for-loop if Hibernate were used better here (see above)
     */
    ArticleAsset assetToPersist = null;
    for (ArticleAsset existingAsset : assets) {
      if (existingAsset.getDoi().equals(assetFileId.getKey())) {
        if (AssetIdentity.hasFile(existingAsset)) {
          assetToPersist = copyArticleFields(existingAsset);
          assets.add(assetToPersist);
        } else {
          assetToPersist = existingAsset;
        }
        break;
      }
    }
    if (assetToPersist == null) {
      throw new RuntimeException();
    }

    /*
     * Pull the data into memory and write it to the file store. Must buffer the whole thing in memory, because we need
     * to know the final size before we can open a stream to the file store. Also, we need to measure the size anyway
     * to record as an asset field.
     */
    String assetFsid = assetFileId.getFsid();
    byte[] assetData = readClientInput(file);
    write(assetData, assetFsid);

    // Set the asset entity's file-specific fields
    assetToPersist.setExtension(assetFileId.getFileExtension());
    assetToPersist.setContentType(assetFileId.getContentType().toString());
    assetToPersist.setSize(assetData.length);

    // Persist to the database
    hibernateTemplate.update(article);

    // Return the result
    return new WriteResult<ArticleAsset>(assetToPersist, WriteResult.Action.CREATED);
  }

  /**
   * Copy all fields defined by article XML into a new asset and return it.
   *
   * @param oldAsset the asset to copy from
   * @return the new asset
   */
  private static ArticleAsset copyArticleFields(ArticleAsset oldAsset) {
    Preconditions.checkNotNull(oldAsset);
    ArticleAsset newAsset = new ArticleAsset();
    newAsset.setDoi(oldAsset.getDoi());
    newAsset.setTitle(oldAsset.getTitle());
    newAsset.setDescription(oldAsset.getDescription());
    return newAsset;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void overwrite(InputStream fileContent, AssetFileIdentity id) throws IOException, FileStoreException {
    ArticleAsset asset = (ArticleAsset) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria.forClass(ArticleAsset.class)
            .add(Restrictions.eq("doi", id.getKey()))
            .add(Restrictions.eq("extension", id.getFileExtension()))
        ));
    if (asset == null) {
      throw new RestClientException("Asset not found at: " + id, HttpStatus.NOT_FOUND);
    }

    String fsid = id.getFsid();
    byte[] assetData = readClientInput(fileContent);
    write(assetData, fsid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream read(AssetFileIdentity assetId) {
    if (!assetExistsAt(assetId)) {
      throw reportNotFound(assetId);
    }
    try {
      return fileStoreService.getFileInStream(assetId.getFsid());
    } catch (FileStoreException e) {
      String message = String.format("Asset not found at DOI \"%s\" with extension \"%s\"",
          assetId.getIdentifier(), assetId.getFileExtension());
      throw new RestClientException(message, HttpStatus.NOT_FOUND, e);
    }
  }

  @Override
  public void readMetadata(HttpServletResponse response, AssetIdentity id, MetadataFormat format)
      throws IOException {
    assert format == MetadataFormat.JSON;
    @SuppressWarnings("unchecked") List<ArticleAsset> assets = ((List<ArticleAsset>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(ArticleAsset.class)
            .add(Restrictions.eq("doi", id.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    if (assets.isEmpty()) {
      throw reportNotFound(id);
    }
    writeJsonToResponse(response, assets);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(AssetFileIdentity assetId) throws FileStoreException {
    ArticleAsset asset = (ArticleAsset) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(ArticleAsset.class)
            .add(Restrictions.eq("doi", assetId.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    if (asset == null) {
      throw reportNotFound(assetId);
    }
    String fsid = assetId.getFsid(); // make sure we get a valid FSID, as an additional check before deleting anything

    hibernateTemplate.delete(asset);
    fileStoreService.deleteFile(fsid);
  }

}
