/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2013 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.content.xml;

import com.google.common.collect.ImmutableMap;
import org.w3c.dom.Node;

import java.util.List;

/**
 * Represents the manifest of an article .zip archive.
 */
public class ManifestXml extends AbstractXpathReader {

  /**
   * Constructor.
   *
   * @param xml the XML content of the manifest file
   */
  public ManifestXml(Node xml) {
    super(xml);
  }

  /**
   * @return the name of the file in the zip archive that is the XML article
   */
  public String getArticleXml() {
    return readString("//article/@main-entry");
  }

  /**
   * @return the URI of the "striking image" associated with this article
   */
  public String getStrkImgURI() {
    return readString("//object[@strkImage='True']/@uri");
  }

  private transient ImmutableMap<String, String> uriMap;

  public String getUriForFile(String filename) {
    ImmutableMap<String, String> uriMap = (this.uriMap == null) ? (this.uriMap = buildUriMap()) : this.uriMap;
    return uriMap.get(filename);
  }

  private ImmutableMap<String, String> buildUriMap() {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    List<Node> objectNodes = readNodeList("//article|//object");
    for (Node objectNode : objectNodes) {
      // TODO: Is there an efficient way to pull this logic into an XPath query?
      String uri = readString("@uri", objectNode);
      List<Node> representationNode = readNodeList("child::representation", objectNode);
      for (Node reprNode : representationNode) {
        String filename = readString("@entry", reprNode);
        builder.put(filename, uri);
      }
    }
    return builder.build();
  }

}
