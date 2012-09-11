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

import org.ambraproject.admin.RestClientException;
import org.ambraproject.admin.controller.ArticleSpaceId;
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

  private boolean assetExistsAt(ArticleSpaceId id) {
    DetachedCriteria criteria = DetachedCriteria.forClass(ArticleAsset.class)
        .add(Restrictions.eq("doi", id.getKey()));
    return exists(criteria);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void create(InputStream file, ArticleSpaceId assetId) throws FileStoreException, IOException {
    final ArticleSpaceId articleId = assetId.getParent();
    if (assetExistsAt(assetId)) {
      throw new RestClientException("Can't create asset; DOI already exists", HttpStatus.METHOD_NOT_ALLOWED);
    }

    // Get these first to fail faster in case of client error
    String articleFsid = articleId.getFsid();
    String assetFsid = assetId.getFsid();


    Article article = (Article) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Article.class)
            .add(Restrictions.eq("doi", articleId.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    if (article == null) {
      String message = "Cannot attach asset to article; no article found at DOI: " + articleId.getDoi();
      throw new RestClientException(message, HttpStatus.NOT_FOUND);
    }

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

    ArticleAsset asset;
    try {
      asset = new AssetXml(articleXml, assetId).build(new ArticleAsset());
    } catch (XmlContentException e) {
      throw new RestClientException(e.getMessage(), HttpStatus.BAD_REQUEST, e);
    }
    hibernateTemplate.save(asset);

    byte[] assetData = readClientInput(file);
    write(assetData, assetFsid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream read(ArticleSpaceId assetId) throws FileStoreException {
    if (!assetExistsAt(assetId)) {
      throw reportNotFound(assetId.getFilePath());
    }
    return fileStoreService.getFileInStream(assetId.getFsid());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void update(InputStream file, ArticleSpaceId assetId) throws FileStoreException, IOException {
    if (!assetExistsAt(assetId)) {
      throw reportNotFound(assetId.getFilePath());
    }
    String assetFsid = assetId.getFsid(); // make sure this is valid before reading the stream
    byte[] assetData = readClientInput(file);
    write(assetData, assetFsid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(ArticleSpaceId assetId) throws FileStoreException {
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
