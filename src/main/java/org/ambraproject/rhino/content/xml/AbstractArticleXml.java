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
import org.ambraproject.rhino.util.StringReplacer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.regex.Pattern;

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
  protected static final ImmutableSet<String> ASSET_WITH_HREF = ImmutableSet.of("supplementary-material", "inline-formula");

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
      return null;
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
    if (attributes != null) {
      Node hrefAttr = attributes.getNamedItem("xlink:href");
      if (hrefAttr != null) {
        return hrefAttr.getTextContent();
      }
    }

    // If href wasn't found, seek it recursively in the child nodes.
    // This is a normal case for <inline-formula id="..."><inline-graphic xlink:href="..."/></inline-formula>
    for (Node child : NodeListAdapter.wrap(assetNode.getChildNodes())) {
      String fromChild = parseAssetWithHref(child);
      if (fromChild != null) {
        return fromChild;
      }
    }
    return null;
  }

  // Legal values for the "name-style" attribute of a <name> node
  private static final String WESTERN_NAME_STYLE = "western";
  private static final String EASTERN_NAME_STYLE = "eastern";

  private static final Joiner NAME_JOINER = Joiner.on(' ').skipNulls();

  /**
   * Parse a person's name from an article XML node. The returned object is useful for populating a {@link
   * org.ambraproject.models.ArticlePerson} or {@link org.ambraproject.models.CitedArticlePerson}.
   * <p/>
   * This method expects to find a "name-style" attribute and "surname" subnode. The "given-names" and "suffix" subnodes
   * are optional. Omitted nodes are represented by an empty string.
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

    String[] fullNameParts;
    if (WESTERN_NAME_STYLE.equals(nameStyle)) {
      fullNameParts = new String[]{givenName, surname, suffix};
    } else if (EASTERN_NAME_STYLE.equals(nameStyle)) {
      fullNameParts = new String[]{surname, givenName, suffix};
    } else {
      throw new XmlContentException("Invalid name-style: " + nameStyle);
    }

    String fullName = NAME_JOINER.join(fullNameParts);
    givenName = Strings.nullToEmpty(givenName);
    suffix = Strings.nullToEmpty(suffix);
    return new PersonName(fullName, givenName, surname, suffix);
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
   * only leading whitespace on a line are deleted; other whitespace may be changed. Markup characters are escaped.
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
          appendTextNode(nodeContent, child);
          break;
        case Node.ELEMENT_NODE:
          appendElementNode(nodeContent, child);
          break;
        default:
          log.warn("Skipping node (name={}, type={})", child.getNodeName(), child.getNodeType());
      }
    }
    return nodeContent;
  }

  /*
   * This is how Ambra Admin matches XML text nodes that are assumed to be non-text-significant whitespace between
   * structural XML elements. Technically it's not 100% safe (it could conceivably remove space from between words), but
   * we'll reproduce the behavior for now to match the Admin test data.
   */
  private static final Pattern UNWANTED_WHITESPACE = Pattern.compile("\\n[ \\t]*");

  private static final StringReplacer XML_CHAR_ESCAPES = StringReplacer.builder()
      .add("&", "&amp;")
      .add("<", "&lt;")
      .add(">", "&gt;")
      .build();

  private static void appendTextNode(StringBuilder nodeContent, Node child) {
    String text = child.getNodeValue();
    if (UNWANTED_WHITESPACE.matcher(text).matches()) {
      return;
    }
    text = CharMatcher.WHITESPACE.collapseFrom(text, ' ');
    text = XML_CHAR_ESCAPES.replace(text);
    nodeContent.append(text);
  }

  private static void appendElementNode(StringBuilder nodeContent, Node child) {
    String nodeName = child.getNodeName();
    nodeContent.append('<').append(nodeName);
    List<Node> attributes = NodeListAdapter.wrap(child.getAttributes());

    // Search for xlink attributes and declare the xlink namespace if found
    // TODO Better way? This is probably a symptom of needing to use a proper XML library here in the first place.
    for (Node attribute : attributes) {
      if (attribute.getNodeName().startsWith("xlink:")) {
        nodeContent.append(" xmlns:xlink=\"http://www.w3.org/1999/xlink\"");
        break;
      }
    }

    for (Node attribute : attributes) {
      nodeContent.append(' ').append(attribute.toString());
    }

    nodeContent.append('>');
    buildTextWithMarkup(nodeContent, child);
    nodeContent.append("</").append(nodeName).append('>');
  }

}

