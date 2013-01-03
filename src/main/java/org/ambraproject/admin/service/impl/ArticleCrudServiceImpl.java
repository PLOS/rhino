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

package org.ambraproject.admin.service.impl;

import com.google.common.base.Optional;
import org.ambraproject.admin.content.xml.ArticleXml;
import org.ambraproject.admin.content.xml.XmlContentException;
import org.ambraproject.admin.identity.ArticleIdentity;
import org.ambraproject.admin.identity.DoiBasedIdentity;
import org.ambraproject.admin.rest.MetadataFormat;
import org.ambraproject.admin.rest.RestClientException;
import org.ambraproject.admin.service.ArticleCrudService;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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
    return (Article) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Article.class)
            .add(Restrictions.eq("doi", id.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public WriteResult write(InputStream file, Optional<ArticleIdentity> suppliedId, WriteMode mode) throws IOException, FileStoreException {
    if (mode == null) {
      mode = WriteMode.WRITE_ANY;
    }

    byte[] xmlData = readClientInput(file);
    ArticleXml xml = new ArticleXml(parseXml(xmlData));
    ArticleIdentity doi;
    try {
      doi = xml.readDoi();
    } catch (XmlContentException e) {
      throw complainAboutXml(e);
    }

    if (suppliedId.isPresent() && !doi.equals(suppliedId.get())) {
      String message = String.format("Article XML with DOI=\"%s\" uploaded to mismatched address: \"%s\"",
          doi.getIdentifier(), suppliedId.get().getIdentifier());
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }
    String fsid = doi.forXmlAsset().getFsid(); // do this first, to fail fast if the DOI is invalid

    Article article = findArticleById(doi);
    final boolean creating = (article == null);
    if ((creating && mode == WriteMode.UPDATE_ONLY) || (!creating && mode == WriteMode.CREATE_ONLY)) {
      String messageStub = (creating ?
          "Can't update; article does not exist at " : "Can't create; article already exists at ");
      throw new RestClientException(messageStub + doi.getIdentifier(), HttpStatus.METHOD_NOT_ALLOWED);
    }
    if (creating) {
      article = new Article();
      article.setDoi(doi.getKey());
    }

    try {
      article = xml.build(article);
    } catch (XmlContentException e) {
      throw complainAboutXml(e);
    }

    if (creating) {
      hibernateTemplate.save(article);
    } else {
      hibernateTemplate.update(article);
    }
    write(xmlData, fsid);
    return (creating ? WriteResult.CREATED : WriteResult.UPDATED);
  }

  private static RestClientException complainAboutXml(XmlContentException e) {
    String msg = "Error in submitted XML";
    String nestedMsg = e.getMessage();
    if (StringUtils.isNotBlank(nestedMsg)) {
      msg = msg + " -- " + nestedMsg;
    }
    return new RestClientException(msg, HttpStatus.BAD_REQUEST, e);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream read(ArticleIdentity id) throws FileStoreException {
    if (!articleExistsAt(id)) {
      throw reportNotFound(id);
    }

    // TODO Can an invalid request cause this to throw FileStoreException? If so, wrap in RestClientException.
    return fileStoreService.getFileInStream(id.forXmlAsset().getFsid());
  }

  @Override
  public String readMetadata(DoiBasedIdentity id, MetadataFormat format) {
    assert format == MetadataFormat.JSON;
    Article article = (Article) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria.forClass(Article.class)
            .add(Restrictions.eq("doi", id.getKey()))
            .setFetchMode("assets", FetchMode.JOIN)
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    if (article == null) {
      throw reportNotFound(id);
    }
    return entityGson.toJson(article);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(ArticleIdentity id) throws FileStoreException {
    Article article = findArticleById(id);
    if (article == null) {
      throw reportNotFound(id);
    }
    String fsid = id.forXmlAsset().getFsid(); // make sure we get a valid FSID, as an additional check before deleting anything

    hibernateTemplate.delete(article);
    fileStoreService.deleteFile(fsid);
  }

}
