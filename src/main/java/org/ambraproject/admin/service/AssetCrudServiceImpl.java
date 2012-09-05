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

  /**
   * {@inheritDoc}
   */
  @Override
  public void create(InputStream file, String assetDoi, String articleDoi) throws FileStoreException, IOException {
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

    /*
     * TODO: Replace this placeholder
     *
     * Will need either:
     *     to extend the controller to get this value explicitly from the client;
     *     to extend the controller to infer this value from the request header; or
     *     to infer it somehow from the article XML or file content itself.
     */
    String fileExtension = "";
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

}
