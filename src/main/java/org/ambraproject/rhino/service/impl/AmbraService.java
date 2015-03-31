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
import com.google.common.base.Strings;
import com.google.gson.Gson;
import org.ambraproject.models.Journal;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.service.article.ArticleService;
import org.ambraproject.service.syndication.SyndicationService;
import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.plos.crepo.model.RepoObject;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public abstract class AmbraService {

  @Autowired
  protected HibernateTemplate hibernateTemplate;

  @Autowired
  protected ContentRepoService contentRepoService;

  @Autowired
  protected Gson entityGson;

  @Autowired
  protected ArticleService articleService;

  @Autowired
  protected SyndicationService syndicationService;

  /**
   * Check whether a distinct entity exists.
   *
   * @param criteria the criteria describing a distinct entity
   * @return {@code true} if the described entity exists and {@code false} otherwise
   */
  protected boolean exists(DetachedCriteria criteria) {
    long count = (Long) DataAccessUtils.requiredSingleResult((List<?>)
        hibernateTemplate.findByCriteria(criteria
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
                .setProjection(Projections.rowCount())
        ));
    return count > 0L;
  }

  protected static DetachedCriteria journalCriteria() {
    return DetachedCriteria.forClass(Journal.class)
        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        .setFetchMode("volumes", FetchMode.JOIN)
        .setFetchMode("volumes.issues", FetchMode.JOIN)
        .setFetchMode("articleList", FetchMode.JOIN)
        .addOrder(Order.asc("journalKey"));
  }

  public static RestClientException reportNotFound(DoiBasedIdentity id) {
    String message = "Item not found at the provided ID: " + id;
    return new RestClientException(message, HttpStatus.NOT_FOUND);
  }

  /**
   * Read a client-provided stream into memory. Report it as a client error if the stream cannot be read. Closes the
   * stream.
   *
   * @param input an input stream from a RESTful request
   * @return a byte array of the input stream contents
   */
  protected static byte[] readClientInput(InputStream input) {
    Preconditions.checkNotNull(input);
    try {
      try {
        return IOUtils.toByteArray(input);
      } finally {
        input.close();
      }
    } catch (IOException e) {
      String message = "Error reading provided file: " + e.getMessage();
      throw new RestClientException(message, HttpStatus.BAD_REQUEST, e);
    }
  }

  /**
   * Write a raw asset to the file store. If something is already stored at the same ID, it is overwritten; else, a new
   * file is created.
   *
   * @param fileData the data to write, as raw bytes
   * @param identity the asset identity
   * @throws IOException
   */
  protected void write(byte[] fileData, AssetFileIdentity identity) throws IOException {
    RepoObject repoObject = new RepoObject.RepoObjectBuilder(identity.toString())
        .byteContent(fileData)
        .contentType(identity.inferContentType().toString())
        .downloadName(identity.getFileName())
        .build();
    contentRepoService.autoCreateRepoObject(repoObject);
  }

  protected void deleteAssetFile(AssetFileIdentity identity) {
    contentRepoService.deleteLatestRepoObject(identity.toString()); // TODO: Need to delete all versions?
  }

  protected static Document parseXml(byte[] bytes) throws IOException, RestClientException {
    try (InputStream stream = new ByteArrayInputStream(bytes)) {
      return parseXml(stream);
    }
  }

  /**
   * Parse client-provided XML. Errors are handled according to whether they most likely were caused by the client or
   * the server.
   *
   * @param stream an input stream containing an XML document as raw bytes
   * @return the XML document parsed from the stream
   * @throws IOException         if the stream cannot be read
   * @throws RestClientException if the stream does not contain valid XML
   */
  protected static Document parseXml(InputStream stream) throws IOException, RestClientException {
    Preconditions.checkNotNull(stream);
    try {
      // Get a new DocumentBuilder every time because because it isn't thread-safe
      return newDocumentBuilder().parse(stream);
    } catch (SAXException e) {
      String message = "Invalid XML";
      String causeMessage = e.getMessage();
      if (!Strings.isNullOrEmpty(causeMessage)) {
        message = message + ": " + causeMessage;
      }
      throw new RestClientException(message, HttpStatus.BAD_REQUEST, e);
    } finally {
      stream.close();
    }
  }

  /**
   * Construct a non-validating document builder. We assume that we don't want to connect to remote servers to validate
   * except with a specific reason.
   *
   * @return a new document builder
   */
  private static DocumentBuilder newDocumentBuilder() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    // at a minimum the document builder needs to be namespace aware
    factory.setNamespaceAware(true);
    factory.setValidating(false);
    try {
      factory.setFeature("http://xml.org/sax/features/validation", false);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      return factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Loads a single entity from the datastore based on some unique property.
   *
   * @param hql Hibernate Query Language string specifying the select.  Should have a single parameter corresponding to
   *            id.  Example: "from Foo where fooID = ?"
   * @param id  unique identifier for the entity
   * @param <S> type of the identifier
   * @param <T> type of the entity being queried/returned
   * @return the retrieved entity
   * @throws RestClientException if no entity is found matching the given ID
   */
  protected <S, T> T findSingleEntity(String hql, S id) {
    List<T> results = hibernateTemplate.find(hql, id);
    if (results.isEmpty()) {
      String message = "Item not found at the provided ID: " + id;
      throw new RestClientException(message, HttpStatus.NOT_FOUND);
    } else if (results.size() > 1) {

      // Should never happen.
      throw new RuntimeException("Multiple entities found for " + id);
    }
    return results.get(0);
  }
}
