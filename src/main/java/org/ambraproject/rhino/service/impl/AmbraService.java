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
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.rest.RestClientException;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

public abstract class AmbraService {

  @Autowired
  protected HibernateTemplate hibernateTemplate;

  @Autowired
  protected ContentRepoService contentRepoService;

  @Autowired
  protected Gson entityGson;

  @Autowired
  protected RuntimeConfiguration runtimeConfiguration;

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
  public static DocumentBuilder newDocumentBuilder() {
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


}
