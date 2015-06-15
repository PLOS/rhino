package org.ambraproject.rhino;

import com.google.common.io.ByteStreams;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.XmlContentException;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.apache.commons.io.output.WriterOutputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utilities for creating test ingestibles.
 */
public class IngestibleUtil {

  public static DocumentBuilder newDocumentBuilder() {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
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

  private static Document buildManifest(ArticleIdentity identity) {
    Document manifest = newDocumentBuilder().newDocument();
    Element manifestElement = (Element) manifest.appendChild(manifest.createElement("manifest"));
    Element articleBundle = (Element) manifestElement.appendChild(manifest.createElement("articleBundle"));

    Element articleElement = (Element) articleBundle.appendChild(manifest.createElement("article"));
    String xmlFileName = identity.forXmlAsset().getFileName();
    articleElement.setAttribute("uri", identity.getKey());
    articleElement.setAttribute("main-entry", xmlFileName);

    Element mainEntryRepresentation = (Element) articleElement.appendChild(manifest.createElement("representation"));
    mainEntryRepresentation.setAttribute("name", "XML");
    mainEntryRepresentation.setAttribute("entry", xmlFileName);

    return manifest;
  }

  public static InputStream buildMockIngestible(InputStream xml) throws IOException {
    byte[] xmlData;
    try {
      xmlData = ByteStreams.toByteArray(xml);
    } finally {
      xml.close();
    }

    DocumentBuilder documentBuilder = newDocumentBuilder();
    Document document;
    try {
      document = documentBuilder.parse(new ByteArrayInputStream(xmlData));
    } catch (SAXException e) {
      throw new RuntimeException(e);
    }
    ArticleXml article = new ArticleXml(document);
    ArticleIdentity identity;
    try {
      identity = article.readDoi();
    } catch (XmlContentException e) {
      throw new RuntimeException(e);
    }
    String xmlFileName = identity.forXmlAsset().getFileName();

    Document manifest = buildManifest(identity);

    ByteArrayOutputStream zipData = new ByteArrayOutputStream();
    try (ZipOutputStream zipStream = new ZipOutputStream(zipData)) {
      zipStream.putNextEntry(new ZipEntry(xmlFileName));
      zipStream.write(xmlData);
      zipStream.closeEntry();

      zipStream.putNextEntry(new ZipEntry("MANIFEST.xml"));
      try {
        TransformerFactory.newInstance().newTransformer().transform(new DOMSource(manifest), new StreamResult(zipStream));
      } catch (TransformerException e) {
        throw new RuntimeException(e);
      }
      zipStream.closeEntry();
    }
    return new ByteArrayInputStream(zipData.toByteArray());
  }

}
