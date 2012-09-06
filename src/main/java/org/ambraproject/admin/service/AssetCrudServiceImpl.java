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

  private boolean assetExistsAt(String doi) {
    DetachedCriteria criteria = DetachedCriteria.forClass(ArticleAsset.class)
        .add(Restrictions.eq("doi", doi));
    return exists(criteria);
  }

  /*
   * Temporary placeholder value for the file extension of all assets.
   * TODO: Replace this placeholder
   *
   * Since this project's design involves generalizing the role of assets in the data model, the need for this method
   * might be removed outright. But assuming that this service still needs to know the file type of asset data, then it
   * will be necessary:
   *     to extend the controller to get this value explicitly from the client, and pass it to the service;
   *     to extend the controller to infer this value from the request header, and pass it to the service; or
   *     to have the service infer it somehow from the article XML or file content itself.
   */
  private String getFileExtension() {
    return "";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void create(InputStream file, String assetDoi, String articleDoi) throws FileStoreException, IOException {
    if (assetExistsAt(assetDoi)) {
      throw new RestClientException("Can't create asset; DOI already exists", HttpStatus.METHOD_NOT_ALLOWED);
    }
    Article article = (Article) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Article.class)
            .add(Restrictions.eq("doi", articleDoi))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    if (article == null) {
      String message = "Cannot attach asset to article; no article found at DOI: " + articleDoi;
      throw new RestClientException(message, HttpStatus.NOT_FOUND);
    }

    String articleFsid = findFsidForArticleXml(articleDoi);
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

    String fileExtension = getFileExtension();
    String assetFsid = findFsid(assetDoi, fileExtension);

    ArticleAsset asset;
    try {
      asset = new AssetXml(articleXml, assetDoi, fileExtension).build(new ArticleAsset());
    } catch (XmlContentException e) {
      throw new RestClientException(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
    hibernateTemplate.save(asset);

    byte[] assetData = readClientInput(file);
    write(assetData, assetFsid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream read(String assetDoi) throws FileStoreException {
    if (!assetExistsAt(assetDoi)) {
      throw reportNotFound(assetDoi);
    }
    String fileExtension = getFileExtension();
    String assetFsid = findFsid(assetDoi, fileExtension);
    return fileStoreService.getFileInStream(assetFsid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void update(InputStream file, String assetDoi) throws FileStoreException, IOException {
    if (!assetExistsAt(assetDoi)) {
      throw reportNotFound(assetDoi);
    }
    String fileExtension = getFileExtension();
    String assetFsid = findFsid(assetDoi, fileExtension);
    byte[] assetData = readClientInput(file);
    write(assetData, assetFsid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(String assetDoi) throws FileStoreException {
    ArticleAsset asset = (ArticleAsset) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(ArticleAsset.class)
            .add(Restrictions.eq("doi", assetDoi))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    if (asset == null) {
      throw reportNotFound(assetDoi);
    }
    hibernateTemplate.delete(asset);

    String fileExtension = getFileExtension();
    String assetFsid = findFsid(assetDoi, fileExtension);
    fileStoreService.deleteFile(assetFsid);
  }

}
