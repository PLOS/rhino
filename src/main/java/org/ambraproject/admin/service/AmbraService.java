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


import com.google.common.base.Preconditions;
import org.ambraproject.admin.RestClientException;
import org.ambraproject.filestore.FSIDMapper;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.filestore.FileStoreService;
import org.apache.commons.io.IOUtils;
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
import java.io.OutputStream;

public abstract class AmbraService {

  @Autowired
  protected HibernateTemplate hibernateTemplate;

  @Autowired
  protected FileStoreService fileStoreService;


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
      return IOUtils.toByteArray(input);
    } catch (IOException e) {
      throw new RestClientException("Could not read provided file", HttpStatus.BAD_REQUEST, e);
    } finally {
      try {
        input.close();
      } catch (IOException e) {
        throw new RestClientException("Error closing file stream from client", HttpStatus.BAD_REQUEST, e);
      }
    }
  }

  /**
   * Produce a file store ID from a client-supplied DOI.
   *
   * @param doi           the DOI of an object
   * @param fileExtension the file extension that denotes the type of the data to be stored
   * @return the FSID for the digital object
   * @throws RestClientException if the DOI can't be parsed and converted into an FSID
   */
  protected static String findFsid(String doi, String fileExtension) {
    String fsid = FSIDMapper.doiTofsid(doi, fileExtension);
    if (fsid.isEmpty()) {
      throw new RestClientException("DOI does not match expected format", HttpStatus.BAD_REQUEST);
    }
    return fsid;
  }

  /**
   * Produce the file store ID for an article's base XML file.
   *
   * @param doi the DOI of an article
   * @return the FSID for the article's XML file
   * @throws RestClientException if the DOI can't be parsed and converted into an FSID
   */
  protected static String findFsidForArticleXml(String doi) {
    return findFsid(doi, "XML");
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
    try {
      output = fileStoreService.getFileOutStream(fsid, fileData.length);
      output.write(fileData);
    } finally {
      IOUtils.closeQuietly(output);
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
    try {
      DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      return documentBuilder.parse(stream);
    } catch (ParserConfigurationException e) {
      throw new RuntimeException();
    } catch (SAXException e) {
      throw new RestClientException("Invalid XML", HttpStatus.BAD_REQUEST, e);
    } finally {
      IOUtils.closeQuietly(stream);
    }
  }

}
