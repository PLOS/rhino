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
import org.ambraproject.admin.xpath.ArticleXml;
import org.ambraproject.admin.xpath.XmlContentException;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Service implementing _c_reate, _r_ead, _u_pdate, and _d_elete operations on article entities and files.
 */
public class ArticleCrudServiceImpl extends AmbraService implements ArticleCrudService {

  private static final Logger log = LoggerFactory.getLogger(ArticleCrudServiceImpl.class);

  private boolean articleExistsAt(String doi) {
    DetachedCriteria criteria = DetachedCriteria.forClass(Article.class)
        .add(Restrictions.eq("doi", doi));
    return exists(criteria);
  }

  /**
   * Create an article entity to represent the metadata provided in the article's XML file.
   *
   * @param xmlData data from the XML file for the new article
   * @param doi     the article's DOI, according to the action that wants to create the article
   * @return the new article object
   */
  private Article prepareMetadata(byte[] xmlData, String doi) {
    InputStream xmlStream = null;
    Document xml;
    try {
      xmlStream = new ByteArrayInputStream(xmlData);
      xml = parseXml(xmlStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(xmlStream);
    }

    return prepareMetadata(xml, doi);
  }

  /**
   * Read metadata from an XML file into a new article representation.
   *
   * @param xml the parsed XML for the new article
   * @param doi the article's DOI, according to the action that wants to create the article
   * @return the new article object
   */
  private Article prepareMetadata(Document xml, String doi) {
    Article article = new Article();
    article.setDoi(doi);

    try {
      article = new ArticleXml(xml).build(article);
    } catch (XmlContentException e) {
      String msg = "Error in submitted XML";
      String nestedMsg = e.getMessage();
      if (StringUtils.isNotBlank(nestedMsg)) {
        msg = msg + ": " + nestedMsg;
      }
      throw new RestClientException(msg, HttpStatus.BAD_REQUEST, e);
    }

    return article;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void create(InputStream file, ArticleSpaceId id) throws IOException, FileStoreException {
    if (articleExistsAt(id.getKey())) {
      throw new RestClientException("Can't create article; DOI already exists", HttpStatus.METHOD_NOT_ALLOWED);
    }
    String fsid = findFsid(id); // do this first, to fail fast if the DOI is invalid
    byte[] xmlData = readClientInput(file);

    Article article = prepareMetadata(xmlData, id.getKey());
    hibernateTemplate.save(article);

    write(xmlData, fsid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream read(ArticleSpaceId id) throws FileStoreException {
    if (!articleExistsAt(id.getKey())) {
      throw reportNotFound(id.getDoi());
    }
    String fsid = findFsid(id);

    // TODO Can an invalid request cause this to throw FileStoreException? If so, wrap in RestClientException.
    return fileStoreService.getFileInStream(fsid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void update(InputStream file, ArticleSpaceId id) throws IOException, FileStoreException {
    if (!articleExistsAt(id.getKey())) {
      throw reportNotFound(id.getDoi());
    }
    write(readClientInput(file), findFsid(id));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(ArticleSpaceId id) throws FileStoreException {
    Article article = (Article) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Article.class)
            .add(Restrictions.eq("doi", id.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    if (article == null) {
      throw reportNotFound(id.getDoi());
    }

    hibernateTemplate.delete(article);
    fileStoreService.deleteFile(findFsid(id));
  }

}
