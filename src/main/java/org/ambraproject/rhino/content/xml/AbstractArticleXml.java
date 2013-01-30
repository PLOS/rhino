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

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.ambraproject.models.AmbraEntity;
import org.ambraproject.rhino.content.PersonName;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.util.NodeListAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.UnsupportedEncodingException;
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

  /**
   * Parse a person's name from an article XML node. The returned object is useful for populating a {@link
   * org.ambraproject.models.ArticlePerson} or {@link org.ambraproject.models.CitedArticlePerson}.
   * <p/>
   * This method expects to find a "name-style" attribute and "surname" and "given-names" subnodes. The "suffix" subnode
   * is optional. The suffix will be represented by an empty string if the suffix node is omitted. In some cases, an
   * empty suffix will need to be manually changed to null -- see {@link CitedArticleXml#emptySuffixToNull}.
   *
   * @param nameNode the node to parse
   * @return the name
   * @throws XmlContentException if an expected field is omitted
   */
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
    suffix = Strings.nullToEmpty(suffix);

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
   * Preconditions: all arguments are non-null; firstName and lastName are non-empty; suffix may be empty
   */
  private static String buildFullName(String firstName, String lastName, String suffix) {
    boolean hasSuffix = !suffix.isEmpty();
    int expectedLength = 2 + firstName.length() + lastName.length() + (hasSuffix ? suffix.length() : -1);
    StringBuilder name = new StringBuilder(expectedLength);
    name.append(firstName).append(' ').append(lastName);
    if (hasSuffix) {
      name.append(' ').append(suffix);
    }
    return name.toString();
  }

  /**
   * Encodes a string according to the URI escaping rules as described in section 2 of RFC 3986.
   */
  public static String uriEncode(String s) {

    // This is surprisingly difficult in Java.  You would think that java.net.URLEncoder would
    // do the trick, but it doesn't--it uses HTML form encoding, which is different.  For
    // instance URLEncode encodes a space character as "+" while RFC 3986 states that it
    // should be "%20".
    try {
      return UriUtils.encodeFragment(s, "UTF-8");
    } catch (UnsupportedEncodingException uee) {
      throw new RuntimeException(uee);
    }
  }

  /**
   * Build a text field by partially reconstructing the node's content as XML. The output is text content between the
   * node's two tags, including nested XML tags with attributes, but not this node's outer tags. Text nodes containing
   * only whitespace are deleted.
   * <p/>
   * This method is used instead of an appropriate XML library in order to match the behavior of legacy code, for now.
   *
   * @param node the node containing the text we are retrieving
   * @return the marked-up node contents
   */
  protected static String buildTextWithMarkup(Node node) {
    return buildTextWithMarkup(new StringBuilder(), node).toString();
  }

  private static StringBuilder buildTextWithMarkup(StringBuilder nodeContent, Node node) {
    List<Node> children = NodeListAdapter.wrap(node.getChildNodes());
    for (Node child : children) {
      switch (child.getNodeType()) {
        case Node.TEXT_NODE:
          String text = child.getNodeValue();
          if (!CharMatcher.WHITESPACE.matchesAllOf(text)) {
            nodeContent.append(text);
          }
          break;
        case Node.ELEMENT_NODE:
          String nodeName = child.getNodeName();
          nodeContent.append('<').append(nodeName);
          NamedNodeMap attributes = child.getAttributes();
          for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            nodeContent.append(' ').append(attribute.toString());
          }
          nodeContent.append('>');
          buildTextWithMarkup(nodeContent, child);
          nodeContent.append("</").append(nodeName).append('>');
          break;
        default:
          // Skip the child
      }
    }
    return nodeContent;
  }

}

