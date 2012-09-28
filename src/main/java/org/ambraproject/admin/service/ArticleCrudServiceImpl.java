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
import org.ambraproject.admin.xpath.ArticleXml;
import org.ambraproject.admin.xpath.XmlContentException;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
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

  private boolean articleExistsAt(DoiBasedIdentity id) {
    DetachedCriteria criteria = DetachedCriteria.forClass(Article.class)
        .add(Restrictions.eq("doi", id.getKey()));
    return exists(criteria);
  }

  /**
   * Query for an article by its identifier.
   *
   * @param id the article's identity
   * @return the article, or {@code null} if not found
   */
  private Article findArticleById(DoiBasedIdentity id) {
    return (Article) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Article.class)
            .add(Restrictions.eq("doi", id.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
  }

  /**
   * Modify an article entity to represent the metadata provided in the article's XML file.
   * <p/>
   * The identifier argument provides the DOI according to the REST action that is trying to create the article. It is
   * expected to match the DOI in the XML document, but this must be validated against client error.
   * <p/>
   * This method takes a byte array instead of a stream because the XML data will have to be sent to the file store
   * after it is parsed here, so the stream would need to be read twice.
   *
   * @param article the article entity that will receive the metadata (new and empty if the article is being created)
   * @param xmlData data from the XML file for the new article
   * @param id      the identifier for the article as provided by the client, if any
   * @return the new article object
   */
  private Article prepareMetadata(Article article, byte[] xmlData, Optional<DoiBasedIdentity> id) {
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

    if (id.isPresent()) {
      article.setDoi(id.get().getKey());
    }

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
  public void create(InputStream file, Optional<DoiBasedIdentity> id) throws IOException, FileStoreException {
    Article article = new Article();
    byte[] xmlData = readClientInput(file);
    prepareMetadata(article, xmlData, id);

    String fsid = DoiBasedIdentity.forArticle(article).getFsid();
    hibernateTemplate.save(article);
    write(xmlData, fsid);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UploadResult upload(InputStream file, DoiBasedIdentity id) throws IOException, FileStoreException {
    String fsid = id.getFsid(); // do this first, to fail fast if the DOI is invalid

    Article article = findArticleById(id);
    if (article == null) {
      create(file, Optional.of(id));
      return UploadResult.CREATED;
    }

    byte[] xmlData = readClientInput(file);
    prepareMetadata(article, xmlData, Optional.of(id));
    hibernateTemplate.update(article);

    write(xmlData, fsid);
    return UploadResult.UPDATED;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream read(DoiBasedIdentity id) throws FileStoreException {
    if (!articleExistsAt(id)) {
      throw reportNotFound(id.getFilePath());
    }

    // TODO Can an invalid request cause this to throw FileStoreException? If so, wrap in RestClientException.
    return fileStoreService.getFileInStream(id.getFsid());
  }

  @Override
  public String readMetadata(DoiBasedIdentity id, MetadataFormat format) {
    assert format == MetadataFormat.JSON;
    Article article = (Article) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(DetachedCriteria.forClass(Article.class)
            .add(Restrictions.eq("doi", id.getKey()))
            .setFetchMode("assets", FetchMode.JOIN)
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    if (article == null) {
      throw reportNotFound(id.getIdentifier());
    }
    return entityGson.toJson(article);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(DoiBasedIdentity id) throws FileStoreException {
    Article article = findArticleById(id);
    if (article == null) {
      throw reportNotFound(id.getFilePath());
    }
    String fsid = id.getFsid(); // make sure we get a valid FSID, as an additional check before deleting anything

    hibernateTemplate.delete(article);
    fileStoreService.deleteFile(fsid);
  }

}
