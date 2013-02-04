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
    @SuppressWarnings("unchecked") List<ArticleAsset> assets =
        hibernateTemplate.findByCriteria(DetachedCriteria.forClass(ArticleAsset.class)
            .add(Restrictions.eq("doi", assetFileId.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY));
    if (assets.isEmpty()) {
      throw new RestClientException("No asset ingested with DOI: " + assetFileId.getIdentifier(),
          HttpStatus.NOT_FOUND);
    }

    ArticleAsset existingAsset = assets.get(0);
    boolean updating = (assets.size() == 1 && !AssetIdentity.hasFile(existingAsset));
    ArticleAsset assetToPersist = updating ? existingAsset : copyArticleFields(existingAsset);
    assetToPersist.setExtension(assetFileId.getFileExtension());
    String assetFsid = assetFileId.getFsid();

    if (updating) {
      hibernateTemplate.update(assetToPersist);
    } else {
      hibernateTemplate.save(assetToPersist);
    }

    byte[] assetData = readClientInput(file);
    write(assetData, assetFsid);
    WriteResult.Action actionResult = (updating ? WriteResult.Action.UPDATED : WriteResult.Action.CREATED);
    return new WriteResult<ArticleAsset>(assetToPersist, actionResult);
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
  public String readMetadata(DoiBasedIdentity id, MetadataFormat format) {
    assert format == MetadataFormat.JSON;
    ArticleAsset asset = (ArticleAsset) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(ArticleAsset.class)
            .add(Restrictions.eq("doi", id.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    if (asset == null) {
      throw reportNotFound(id);
    }
    return entityGson.toJson(asset);
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
