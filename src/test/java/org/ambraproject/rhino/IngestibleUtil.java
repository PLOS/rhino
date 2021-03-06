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

package org.ambraproject.rhino;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.service.impl.AmbraService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utilities for creating test ingestibles.
 */
public class IngestibleUtil {

//  /**
//   * Create a file name that uniquely identifies an asset file in an ingestible.
//   */
//  private static String getDummyFileName(ArticleAsset asset) {
//    // Since the file name should be independent of the asset identity anyway, we might as well use consistent nonsense.
//    return Hashing.sha1().newHasher()
//        .putUnencodedChars(asset.getDoi())
//        .putLong(0x51168181d9999aefL)
//        .putUnencodedChars(asset.getExtension())
//        .hash().toString();
//  }
//
//  private static Document buildManifest(ArticleIdentity identity, List<ArticleAsset> expectedAssets) {
//    Document manifest = AmbraService.newDocumentBuilder().newDocument();
//    Element manifestElement = (Element) manifest.appendChild(manifest.createElement("manifest"));
//    Element articleBundle = (Element) manifestElement.appendChild(manifest.createElement("articleBundle"));
//
//    Element articleElement = (Element) articleBundle.appendChild(manifest.createElement("article"));
//    String xmlFileName = identity.forXmlAsset().getFileName();
//    articleElement.setAttribute("uri", identity.getKey());
//    articleElement.setAttribute("main-entry", xmlFileName);
//
//    Element mainEntryRepresentation = (Element) articleElement.appendChild(manifest.createElement("representation"));
//    mainEntryRepresentation.setAttribute("name", "XML");
//    mainEntryRepresentation.setAttribute("entry", xmlFileName);
//
//    Map<String, List<ArticleAsset>> assetGroups = expectedAssets.stream().collect(Collectors.groupingBy(ArticleAsset::getDoi));
//    for (Map.Entry<String, List<ArticleAsset>> entry : assetGroups.entrySet()) {
//      AssetIdentity assetId = AssetIdentity.create(entry.getKey());
//
//      final Element parentElement;
//      boolean isRootAsset = identity.getIdentifier().equals(assetId.getIdentifier());
//      if (isRootAsset) {
//        parentElement = articleElement;
//      } else {
//        parentElement = (Element) articleBundle.appendChild(manifest.createElement("object"));
//        parentElement.setAttribute("uri", assetId.getKey());
//      }
//
//      for (ArticleAsset asset : entry.getValue()) {
//        AssetFileIdentity fileId = AssetFileIdentity.from(asset);
//        if (isRootAsset && fileId.getFileExtension().equals("XML")) {
//          continue; // already created above
//        }
//        Element assetRepresentation = (Element) parentElement.appendChild(manifest.createElement("representation"));
//        assetRepresentation.setAttribute("name", asset.getExtension());
//        assetRepresentation.setAttribute("entry", getDummyFileName(asset));
//      }
//    }
//
//    return manifest;
//  }
//
//  public static InputStream buildMockIngestible(InputStream xml, List<ArticleAsset> expectedAssets) throws IOException {
//    byte[] xmlData;
//    try {
//      xmlData = ByteStreams.toByteArray(xml);
//    } finally {
//      xml.close();
//    }
//
//    DocumentBuilder documentBuilder = AmbraService.newDocumentBuilder();
//    Document document;
//    try {
//      document = documentBuilder.parse(new ByteArrayInputStream(xmlData));
//    } catch (SAXException e) {
//      throw new RuntimeException(e);
//    }
//    ArticleXml article = new ArticleXml(document);
//    ArticleIdentity identity;
//    try {
//      identity = ArticleIdentity.create(article.readDoi().getName());
//    } catch (XmlContentException e) {
//      throw new RuntimeException(e);
//    }
//    String xmlFileName = identity.forXmlAsset().getFileName();
//
//    Document manifest = buildManifest(identity, expectedAssets);
//
//    ByteArrayOutputStream zipData = new ByteArrayOutputStream();
//    try (ZipOutputStream zipStream = new ZipOutputStream(zipData)) {
//      zipStream.putNextEntry(new ZipEntry(xmlFileName));
//      zipStream.write(xmlData);
//      zipStream.closeEntry();
//
//      zipStream.putNextEntry(new ZipEntry("MANIFEST.xml"));
//      try {
//        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(manifest), new StreamResult(zipStream));
//      } catch (TransformerException e) {
//        throw new RuntimeException(e);
//      }
//
//      for (ArticleAsset expectedAsset : expectedAssets) {
//        AssetFileIdentity fileId = AssetFileIdentity.from(expectedAsset);
//        if (identity.getIdentifier().equals(fileId.getIdentifier()) && fileId.getFileExtension().equals("XML")) {
//          continue; // we already created this one above
//        }
//        String fileName = getDummyFileName(expectedAsset);
//        zipStream.putNextEntry(new ZipEntry(fileName));
//        writeDummyFile(zipStream, expectedAsset.getSize());
//        zipStream.closeEntry(); // write empty placeholder file
//      }
//
//      zipStream.closeEntry();
//    }
//    return new ByteArrayInputStream(zipData.toByteArray());
//  }

  private static final byte[] DUMMY_BUFFER = new byte[0x1000];

  /**
   * Write dummy binary content of a given length.
   */
  private static void writeDummyFile(OutputStream zipStream, long length) throws IOException {
    // Copy out of a moderately-sized buffer array to cut down the total number of OutputStream.write calls
    for (long i = 0; i < length / DUMMY_BUFFER.length; i++) {
      zipStream.write(DUMMY_BUFFER);
    }
    zipStream.write(DUMMY_BUFFER, 0, (int) (length % DUMMY_BUFFER.length));
  }

}
