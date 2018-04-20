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

package org.ambraproject.rhino.content.xml;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.article.NlmPerson;
import org.ambraproject.rhino.util.NodeListAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A holder for a piece (node or document) of NLM-format XML, which can be built into an entity.
 *
 * @param <T> the type of entity that can be build from this XML element
 */
public abstract class AbstractArticleXml<T> extends AbstractXpathReader {

  private static final Logger log = LoggerFactory.getLogger(AbstractArticleXml.class);

  protected AbstractArticleXml(Node xml) {
    super(xml);
  }

  /**
   * Build an object from the XML supplied to this object.
   *
   * @throws XmlContentException if the supplied XML omits a required element or does not have the expected structure
   */
  public abstract T build() throws XmlContentException;

  @Override
  protected String sanitize(String text) {
    return (text == null) ? null : CharMatcher.WHITESPACE.trimAndCollapseFrom(text, ' ');
  }

  // Node names that get special handling
  protected static final String GRAPHIC = "graphic";
  protected static final String TABLE_WRAP = "table-wrap";
  protected static final String ALTERNATIVES = "alternatives";
  protected static final String DISP_FORMULA = "disp-formula";

  // The node-names for nodes that can be an asset on their own
  protected static final ImmutableSet<String> ASSET_NODE_NAMES = ImmutableSet.of(
      "supplementary-material", "inline-formula", DISP_FORMULA, GRAPHIC);

  // The node-names for nodes that can be an asset if they have a descendant <graphic> node
  protected static final ImmutableSet<String> GRAPHIC_NODE_PARENTS = ImmutableSet.of(TABLE_WRAP, "fig",
      DISP_FORMULA);

  // An XPath expression that will match any node with one of the name in ASSET_NODE_NAMES.
  protected static final String ASSET_EXPRESSION =
      ASSET_NODE_NAMES.stream().map(nodeName -> "//" + nodeName).collect(Collectors.joining("|"));

  protected Doi getAssetDoi(Node assetNode) {
    String nodeName = assetNode.getNodeName();
    String doi;
    if (GRAPHIC_NODE_PARENTS.contains(nodeName)) {
      doi = readString("object-id[@pub-id-type=\"doi\"]", assetNode);
      if (doi == null && nodeName.equals(DISP_FORMULA)) {
        //disp-formula may be a graphic node parent, or an asset node name
        doi = readHrefAttribute(assetNode);
      }
    } else if (ASSET_NODE_NAMES.contains(nodeName)) {
      doi = readHrefAttribute(assetNode);
    } else {
      String message = String.format("Received a node of type \"%s\"; expected one of: %s",
          nodeName, Iterables.concat(ASSET_NODE_NAMES, GRAPHIC_NODE_PARENTS));
      throw new IllegalArgumentException(message);
    }
    if (doi == null) {
      log.warn("An asset node ({}) does not have DOI as expected", assetNode.getNodeName());
      return null;
    }
    return Doi.create(doi);
  }

  /**
   * Read the "xlink:href" attribute from a node.
   * <p/>
   * TODO: Use XPath instead and handle the XML namespace properly.
   */
  protected static String readHrefAttribute(Node assetNode) {
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
      String fromChild = readHrefAttribute(child);
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

  protected List<NlmPerson> readPersons(List<Node> personNodes) throws XmlContentException {
    return personNodes.stream().map(this::parsePersonName).collect(Collectors.toList());
  }

  /**
   * Parse a person's name from an article XML node. The returned object is useful for populating a
   * {@link NlmPerson}
   * <p/>
   * This method expects to find a "name-style" attribute and "surname" subnode. The "given-names" and "suffix" subnodes
   * are optional. Omitted nodes are represented by an empty string.
   *
   * @param nameNode the node to parse
   * @return the name
   * @throws XmlContentException if an expected field is omitted
   */
  private NlmPerson parsePersonName(Node nameNode) throws XmlContentException {
    String nameStyle = readString("@name-style", nameNode);
    String surname = readString("surname", nameNode);
    String givenName = readString("given-names", nameNode);
    String suffix = readString("suffix", nameNode);

    if (surname == null) {
      throw new XmlContentException("Required surname is omitted from node: " + logNode(nameNode));
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
    return NlmPerson.create(fullName, givenName, surname, suffix);
  }

  /**
   * Encodes a string according to the URI escaping rules as described in section 2 of RFC 3986.
   */
  protected static String uriEncode(String s) {

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

}

