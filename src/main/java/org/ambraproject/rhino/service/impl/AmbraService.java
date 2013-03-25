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


import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;
import com.google.gson.Gson;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.filestore.FileStoreService;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.service.article.ArticleClassifier;
import org.ambraproject.service.article.ArticleService;
import org.ambraproject.service.syndication.SyndicationService;
import org.apache.commons.io.IOUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.List;

public abstract class AmbraService {

  @Autowired
  protected HibernateTemplate hibernateTemplate;

  @Autowired
  protected FileStoreService fileStoreService;

  @Autowired
  protected Gson entityGson;

  @Autowired
  protected ArticleClassifier articleClassifier;

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

  protected RestClientException reportNotFound(DoiBasedIdentity id) {
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
    byte[] data;
    try {
      boolean threw = true;
      try {
        data = IOUtils.toByteArray(input);
        threw = false;
      } finally {
        Closeables.close(input, threw);
      }
    } catch (IOException e) {
      String message = "Error reading provided file: " + e.getMessage();
      throw new RestClientException(message, HttpStatus.BAD_REQUEST, e);
    }
    return data;
  }

  /**
   * Write the base article XML to the file store. If something is already stored at the same file store ID, it is
   * overwritten; else, a new file is created.
   *
   * @param fileData the data to write, as raw bytes
   * @param fsid     the file store ID
   * @throws org.ambraproject.filestore.FileStoreException
   *
   * @throws IOException
   */
  protected void write(byte[] fileData, String fsid) throws FileStoreException, IOException {
    OutputStream output = null;
    boolean threw = true;
    try {
      output = fileStoreService.getFileOutStream(fsid, fileData.length);
      output.write(fileData);
      threw = false;
    } finally {
      Closeables.close(output, threw);
    }
  }

  protected static Document parseXml(byte[] bytes) throws IOException, RestClientException {
    InputStream stream = null;
    boolean threw = true;
    try {
      stream = new ByteArrayInputStream(bytes);
      Document document = parseXml(stream);
      threw = false;
      return document;
    } finally {
      Closeables.close(stream, threw);
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
      throw new RestClientException("Invalid XML", HttpStatus.BAD_REQUEST, e);
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
    factory.setNamespaceAware(false);
    factory.setValidating(false);
    try {
      factory.setFeature("http://xml.org/sax/features/namespaces", false);
      factory.setFeature("http://xml.org/sax/features/validation", false);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      return factory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Write JSON describing an object.
   *
   * @param receiver the receiver object to write to
   * @param entity   the object to describe in JSON
   * @throws IOException if the response can't be written to
   */
  protected void writeJson(ResponseReceiver receiver, Object entity) throws IOException {
    Writer writer = null;
    boolean threw = true;
    try {
      receiver.setCharacterEncoding(Charsets.UTF_8);
      writer = receiver.getWriter();
      writer = new BufferedWriter(writer);
      entityGson.toJson(entity, writer);
      threw = false;
    } finally {
      Closeables.close(writer, threw);
    }
  }

}
