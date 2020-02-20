/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.service.impl;


import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.Gson;
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

  /**
   * Parse client-provided XML. Errors are handled according to whether they most likely were caused by the client or
   * the server.
   *
   * @param stream an input stream containing an XML document as raw bytes
   * @return the XML document parsed from the stream
   * @throws IOException         if the stream cannot be read
   * @throws RestClientException if the stream does not contain valid XML
   */
  public static Document parseXml(InputStream stream) throws IOException, RestClientException {
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
