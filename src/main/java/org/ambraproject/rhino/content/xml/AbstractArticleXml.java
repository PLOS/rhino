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

package org.ambraproject.rhino.content.xml;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.ambraproject.models.AmbraEntity;
import org.ambraproject.rhino.content.PersonName;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.List;

/**
 * A holder for a piece (node or document) of NLM-format XML, which can be built into an entity.
 *
 * @param <T> the type of entity that can be build from this XML element
 */
public abstract class AbstractArticleXml<T extends AmbraEntity> extends XmlToObject<T> {

  private static final Logger log = LoggerFactory.getLogger(AssetXml.class);

  protected AbstractArticleXml(Node xml) {
    super(xml);
  }

  // The node-names for nodes that can be an asset, separated by where to find the DOI
  protected static final ImmutableSet<String> ASSET_WITH_OBJID = ImmutableSet.of("table-wrap", "fig");
  protected static final ImmutableSet<String> ASSET_WITH_HREF = ImmutableSet.of("supplementary-material", "inline-graphic");

  // An XPath expression that will match any node with one of the names above
  private static final String ASSET_EXPRESSION = String.format("//(%s)",
      Joiner.on('|').join(Iterables.concat(ASSET_WITH_OBJID, ASSET_WITH_HREF)));

  /**
   * Get a list containing each node within this object's XML whose name is expected to be associated with an asset
   * entity.
   *
   * @return the list of asset nodes
   */
  protected List<AssetNode> findAllAssetNodes() {
    List<Node> raw = readNodeList(ASSET_EXPRESSION);
    List<AssetNode> wrapped = Lists.newArrayListWithCapacity(raw.size());
    for (Node node : raw) {
      wrapped.add(new AssetNode(node, getAssetDoi(node)));
    }
    return ImmutableList.copyOf(wrapped);
  }

  protected String getAssetDoi(Node assetNode) {
    String nodeName = assetNode.getNodeName();
    String doi;
    if (ASSET_WITH_OBJID.contains(nodeName)) {
      doi = readString("object-id[@pub-id-type=\"doi\"]", assetNode);
    } else if (ASSET_WITH_HREF.contains(nodeName)) {
      doi = parseAssetWithHref(assetNode);
    } else {
      String message = String.format("Received a node of type \"%s\"; expected one of: %s",
          nodeName, ASSET_EXPRESSION);
      throw new IllegalArgumentException(message);
    }
    if (doi == null) {
      log.warn("An asset node ({}) does not have DOI as expected", assetNode.getNodeName());
    }
    return DoiBasedIdentity.removeScheme(doi);
  }

  /*
   * Read the "xlink:href" attribute from a <supplementary-material> or <inline-graphic> node.
   *
   * TODO: Use XPath instead and handle the XML namespace properly.
   */
  private static String parseAssetWithHref(Node assetNode) {
    NamedNodeMap attributes = assetNode.getAttributes();
    if (attributes == null) {
      return null;
    }
    Node hrefAttr = attributes.getNamedItem("xlink:href");
    if (hrefAttr == null) {
      return null;
    }
    return hrefAttr.getTextContent();
  }

  // Legal values for the "name-style" attribute of a <name> node
  private static final String WESTERN_NAME_STYLE = "western";
  private static final String EASTERN_NAME_STYLE = "eastern";

  protected PersonName parsePersonName(Node nameNode)
      throws XmlContentException {
    String nameStyle = readString("@name-style", nameNode);
    String surname = readString("surname", nameNode);
    String givenName = readString("given-names", nameNode);
    String suffix = readString("suffix", nameNode);

    if (surname == null) {
      throw new XmlContentException("Required surname is omitted");
    }
    if (givenName == null) {
      throw new XmlContentException("Required given name is omitted");
    }

    String fullName;
    if (WESTERN_NAME_STYLE.equals(nameStyle)) {
      fullName = buildFullName(givenName, surname, suffix);
    } else if (EASTERN_NAME_STYLE.equals(nameStyle)) {
      fullName = buildFullName(surname, givenName, suffix);
    } else {
      throw new XmlContentException("Invalid name-style: " + nameStyle);
    }

    return new PersonName(fullName, givenName, surname, suffix);
  }

  /*
   * Preconditions: firstName and lastName are both non-null and non-empty; suffix may be null
   */
  private static String buildFullName(String firstName, String lastName, String suffix) {
    boolean hasSuffix = (suffix != null);
    int expectedLength = 2 + firstName.length() + lastName.length() + (hasSuffix ? suffix.length() : -1);
    StringBuilder name = new StringBuilder(expectedLength);
    name.append(firstName).append(' ').append(lastName);
    if (hasSuffix) {
      name.append(' ').append(suffix);
    }
    return name.toString();
  }


}

