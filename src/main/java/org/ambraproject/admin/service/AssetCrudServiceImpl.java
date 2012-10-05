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
import org.ambraproject.admin.RestClientException;
import org.ambraproject.admin.controller.DoiBasedIdentity;
import org.ambraproject.admin.controller.MetadataFormat;
import org.ambraproject.admin.xpath.AssetXml;
import org.ambraproject.admin.xpath.XmlContentException;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;

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
  public WriteResult upload(InputStream file, DoiBasedIdentity assetId, Optional<DoiBasedIdentity> articleIdParam)
      throws FileStoreException, IOException {
    ArticleAsset asset = (ArticleAsset) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(DetachedCriteria.forClass(ArticleAsset.class)
            .add(Restrictions.eq("doi", assetId.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));

    return (asset == null)
        ? create(file, assetId, articleIdParam)
        : update(file, assetId, articleIdParam, asset);
  }

  /*
   * An upload operation that needs to create a new asset. Validate input, then delegate to doUpload.
   */
  private WriteResult create(InputStream file,
                             DoiBasedIdentity assetId,
                             Optional<DoiBasedIdentity> articleIdParam)
      throws FileStoreException, IOException {
    // Require that the user identified the parent article
    if (!articleIdParam.isPresent()) {
      String message = String.format("Asset does not exist with DOI=\"%s\". "
          + "Must provide an assetOf parameter when uploading a new asset.", assetId.getKey());
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }

    // Look up the identified parent article
    DoiBasedIdentity articleId = articleIdParam.get();
    Article article = (Article) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(DetachedCriteria.forClass(Article.class)
            .add(Restrictions.eq("doi", articleId.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    if (article == null) {
      String message = "Could not find article with DOI=" + articleId.getIdentifier();
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }

    // Create the association now; the asset's state will be set in doUpload
    ArticleAsset asset = new ArticleAsset();
    article.getAssets().add(asset);

    doUpload(file, article, asset, assetId);
    return WriteResult.CREATED;
  }

  /*
   * An upload operation that needs to update a preexisting asset. Validate input, then delegate to doUpload.
   */
  private WriteResult update(InputStream file,
                             DoiBasedIdentity assetId,
                             Optional<DoiBasedIdentity> articleIdParam,
                             ArticleAsset asset)
      throws FileStoreException, IOException {
    // Look up the parent article, by the asset's preexisting association
    String assetDoi = asset.getDoi();
    Article article = (Article) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(DetachedCriteria.forClass(Article.class)
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
            .createCriteria("assets").add(Restrictions.eq("doi", assetDoi))
        ));
    if (article == null) {
      throw new IllegalStateException("Orphan asset: " + assetId.getKey());
    }
    DoiBasedIdentity articleId = DoiBasedIdentity.forArticle(article);

    // If the user identified an article, throw an error if it was inconsistent
    if (articleIdParam.isPresent() && !articleIdParam.get().equals(articleId)) {
      String message = String.format(
          "Provided ID for asset's article (%s) is inconsistent with asset's actual article (%s)",
          articleIdParam.get().getIdentifier(), articleId.getIdentifier());
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }

    doUpload(file, article, asset, assetId);
    return WriteResult.UPDATED;
  }

  /*
   * "Payload" behavior common to both create and update operations.
   */
  private void doUpload(InputStream file, Article article, ArticleAsset asset, DoiBasedIdentity assetId)
      throws FileStoreException, IOException {
    // Get these first to fail faster in case of client error
    String assetFsid = assetId.getFsid();
    String articleFsid = DoiBasedIdentity.forArticle(article).getFsid();

    InputStream articleStream = null;
    Document articleXml;
    try {
      articleStream = fileStoreService.getFileInStream(articleFsid);
      articleXml = parseXml(articleStream);
    } catch (IOException e) {
      throw new FileStoreException(e);
    } finally {
      IOUtils.closeQuietly(articleStream);
    }

    try {
      asset = new AssetXml(articleXml, assetId).build(asset);
    } catch (XmlContentException e) {
      throw new RestClientException(e.getMessage(), HttpStatus.BAD_REQUEST, e);
    }
    hibernateTemplate.update(article);

    byte[] assetData = readClientInput(file);
    write(assetData, assetFsid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream read(DoiBasedIdentity assetId) throws FileStoreException {
    if (!assetExistsAt(assetId)) {
      throw reportNotFound(assetId.getFilePath());
    }
    return fileStoreService.getFileInStream(assetId.getFsid());
  }

  @Override
  public String readMetadata(DoiBasedIdentity id, MetadataFormat format) {
    assert format == MetadataFormat.JSON;
    ArticleAsset asset = (ArticleAsset) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(ArticleAsset.class)
            .add(Restrictions.eq("doi", id.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    if (asset == null) {
      reportNotFound(id.getFilePath());
    }
    return entityGson.toJson(asset);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(DoiBasedIdentity assetId) throws FileStoreException {
    ArticleAsset asset = (ArticleAsset) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(ArticleAsset.class)
            .add(Restrictions.eq("doi", assetId.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    if (asset == null) {
      throw reportNotFound(assetId.getFilePath());
    }
    String fsid = assetId.getFsid(); // make sure we get a valid FSID, as an additional check before deleting anything

    hibernateTemplate.delete(asset);
    fileStoreService.deleteFile(fsid);
  }

}
