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

  private Article getParentFor(ArticleAsset asset) {
    throw new RuntimeException("Unimplemented"); // TODO
//    return (Article) DataAccessUtils.uniqueResult(
//        hibernateTemplate.findByCriteria(DetachedCriteria.forClass(Article.class)
//            .add(Restrictions.in("assets", ImmutableList.of(asset)))
//        ));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void upload(InputStream file, DoiBasedIdentity assetId, Optional<DoiBasedIdentity> articleId)
      throws FileStoreException, IOException {
    ArticleAsset asset = (ArticleAsset) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(DetachedCriteria.forClass(ArticleAsset.class)
            .add(Restrictions.eq("doi", assetId.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));

    if (asset == null) {
      // Creating a new asset
      if (!articleId.isPresent()) {
        String message = String.format("Asset does not exist with DOI=\"%s\". "
            + "Must provide an assetOf parameter when uploading a new asset.", assetId.getKey());
        throw new RestClientException(message, HttpStatus.BAD_REQUEST);
      }
      // TODO Verify that articleId refers to an existing article; throw RestClientException if not
      upload(new ArticleAsset(), file, assetId, articleId.get());
    } else {
      // Updating an existing asset
      if (articleId.isPresent()) {
        // TODO Ensure that the given articleId is consistent with the actual parent article
        upload(asset, file, assetId, articleId.get());
      } else {
        DoiBasedIdentity parentArticleId = null; // TODO Look up parent from existing asset
        upload(asset, file, assetId, parentArticleId);
      }
    }

  }

  private void upload(ArticleAsset asset, InputStream file, DoiBasedIdentity assetId, DoiBasedIdentity articleId)
      throws FileStoreException, IOException {
    // Get these first to fail faster in case of client error
    String assetFsid = assetId.getFsid();
    String articleFsid = articleId.getFsid();

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
    hibernateTemplate.save(asset);

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
