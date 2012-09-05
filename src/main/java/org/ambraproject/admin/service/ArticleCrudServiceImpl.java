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
import org.ambraproject.admin.xpath.ArticleXml;
import org.ambraproject.admin.xpath.XmlContentException;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
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
    Long articleCount = (Long) DataAccessUtils.requiredSingleResult(
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Article.class)
            .add(Restrictions.eq("doi", doi))
            .setProjection(Projections.rowCount())
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    return articleCount.longValue() > 0L;
  }

  private RestClientException reportDoiNotFound() {
    return new RestClientException("DOI does not belong to an article", HttpStatus.NOT_FOUND);
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
  public void create(InputStream file, String doi) throws IOException, FileStoreException {
    if (articleExistsAt(doi)) {
      throw new RestClientException("Can't create article; DOI already exists", HttpStatus.METHOD_NOT_ALLOWED);
    }
    String fsid = findFsidForArticleXml(doi); // do this first, to fail fast if the DOI is invalid
    byte[] xmlData = readClientInput(file);

    Article article = prepareMetadata(xmlData, doi);
    hibernateTemplate.save(article);

    write(xmlData, fsid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream read(String doi) throws FileStoreException {
    if (!articleExistsAt(doi)) {
      throw reportDoiNotFound();
    }
    String fsid = findFsidForArticleXml(doi);

    // TODO Can an invalid request cause this to throw FileStoreException? If so, wrap in RestClientException.
    return fileStoreService.getFileInStream(fsid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void update(InputStream file, String doi) throws IOException, FileStoreException {
    if (!articleExistsAt(doi)) {
      throw reportDoiNotFound();
    }
    write(readClientInput(file), findFsidForArticleXml(doi));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(String doi) throws FileStoreException {
    Article article = (Article) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Article.class)
            .add(Restrictions.eq("doi", doi))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    if (article == null) {
      throw reportDoiNotFound();
    }

    hibernateTemplate.delete(article);
    fileStoreService.deleteFile(doi);
  }

}
