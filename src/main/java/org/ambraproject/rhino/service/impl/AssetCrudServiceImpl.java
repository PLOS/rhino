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

import com.google.common.base.Preconditions;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

public class AssetCrudServiceImpl extends AmbraService implements AssetCrudService {

  private static final Logger log = LoggerFactory.getLogger(AssetCrudServiceImpl.class);

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
    if (article == null) {
      String message = "No article found that contains an asset with DOI = " + assetFileId.getIdentifier();
      throw new RestClientException(message, HttpStatus.NOT_FOUND);
    }
    List<ArticleAsset> assets = article.getAssets();

    // Set up the Hibernate entity that we want to modify
    ArticleAsset assetToPersist = createAssetToAdd(assets, assetFileId);

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
   * Examine a persistent collection of existing asset entities and select or create one to represent a new file upload.
   * If there is already a file-less asset with a matching DOI, this method finds and returns it. Else, it creates a new
   * asset with the article-defined data initialized and modifies the given collection by adding it.
   * <p/>
   * It also does two kinds of data validation. If the DOI-and-extension pair being created already exists, throw a
   * client error. If a file-less asset coexists with one or more assets that have file data, log a warning because this
   * should not be possible.
   * <p/>
   * This method is pretty hairy. It should be possible to do all of this with direct queries on the needed assets,
   * without loading and looping over the article's entire list of assets. This would require adding an asset to that
   * list without loading the whole article, which is an open TODO.
   *
   * @param existingAssets the collection of existing assets associated with one article, to be modified
   * @param idToCreate     the identity of the uploaded asset file that needs a new asset entity
   * @return the asset entity to be modified and persisted for this file upload
   * @throws RestClientException if the uploaded file collides with an existing file
   */
  private static ArticleAsset createAssetToAdd(final Collection<ArticleAsset> existingAssets,
                                               final AssetFileIdentity idToCreate) {
    final String keyToCreate = idToCreate.getKey();
    ArticleAsset newAsset = null;
    boolean creatingNewEntity = false;
    for (ArticleAsset existingAsset : existingAssets) {
      if (!keyToCreate.equals(existingAsset.getDoi())) {
        continue; // Skip files that belong to other assets
      }
      if (!AssetIdentity.hasFile(existingAsset)) {
        // This asset is uninitialized.
        if (newAsset != null) {
          // We previously matched an asset that had a file, and expected not to find this file-less one.
          complainAboutUninitializedAsset(idToCreate);
        }
        // We will modify the file-less asset to give it a file. Won't insert a new asset object.
        newAsset = existingAsset;
        creatingNewEntity = false;
      } else if (idToCreate.equals(AssetFileIdentity.from(existingAsset))) {
        String message = "Asset already exists: " + idToCreate;
        throw new RestClientException(message, HttpStatus.METHOD_NOT_ALLOWED);
      } else if (newAsset == null) {
        // Found an existing, initialized asset with a matching DOI.
        // Will insert a new asset object to represent this file, copying the old one's article-defined fields.
        newAsset = copyArticleFields(existingAsset);
        creatingNewEntity = true;
      } else if (!creatingNewEntity) {
        // We previously matched a file-less asset, and expected not to find this one that has a file.
        complainAboutUninitializedAsset(idToCreate);
      }
    }

    if (newAsset == null) {
      String message = String.format(
          "No asset found with DOI=\"%s\" (must appear in an article that has been ingested)",
          idToCreate.getIdentifier());
      throw new RestClientException(message, HttpStatus.METHOD_NOT_ALLOWED);
    }
    if (creatingNewEntity) {
      existingAssets.add(newAsset);
    }
    return newAsset;
  }

  private static void complainAboutUninitializedAsset(AssetFileIdentity identity) {
    log.warn("Illegal data state: An uninitialized ArticleAsset entity coexists "
        + "with one or more asset files on DOI = {}", identity.getIdentifier());
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
