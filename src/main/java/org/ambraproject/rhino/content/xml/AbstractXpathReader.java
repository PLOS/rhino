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
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.ambraproject.rhino.util.NodeListAdapter;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayOutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * A container for a node of XML data that reads it with XPath queries.
 * <p/>
 * Instances of this class are not thread-safe because they hold an instance of {@link XPath} to use.
 */
public abstract class AbstractXpathReader {

  protected Node xml;
  protected final XPath xPath;

  protected AbstractXpathReader(Node xml) {
    this();
    this.xml = Preconditions.checkNotNull(xml);
  }

  protected AbstractXpathReader() {

    // XPath isn't thread-safe, so we need one per instance of this class
    this.xPath = XPathFactory.newInstance().newXPath();
  }

  /**
   * Define default text formatting for this object. By default, just calls {@link org.w3c.dom.Node#getTextContent()}.
   * Override for something different. Returns null on null input.
   *
   * @param node an XML node
   * @return the node's text content with optional formatting
   */
  protected String getTextFromNode(Node node) {
    return (node == null) ? null : node.getTextContent();
  }

  protected String readString(String query) {
    return readString(query, xml);
  }

  protected String readString(String query, Node node) {
    Node stringNode = readNode(query, node);
    if (stringNode == null) {
      return null;
    }
    String text = getTextFromNode(stringNode);
    return (StringUtils.isBlank(text) ? null : text);
  }

  protected Node readNode(String query) {
    return readNode(query, xml);
  }

  protected Node readNode(String query, Node node) {
    try {
      return (Node) xPath.evaluate(query, node, XPathConstants.NODE);
    } catch (XPathExpressionException e) {
      throw new InvalidXPathException(query, e);
    }
  }

  protected List<Node> readNodeList(String query) {
    return readNodeList(query, xml);
  }

  protected List<Node> readNodeList(String query, Node node) {
    NodeList nodeList;
    try {
      nodeList = (NodeList) xPath.evaluate(query, node, XPathConstants.NODESET);
    } catch (XPathExpressionException e) {
      throw new InvalidXPathException(query, e);
    }
    return NodeListAdapter.wrap(nodeList);
  }

  protected List<String> readTextList(String query) {
    List<Node> nodeList = readNodeList(query);
    List<String> textList = Lists.newArrayListWithCapacity(nodeList.size());
    for (Node node : nodeList) {
      textList.add(getTextFromNode(node));
    }
    return textList;
  }


  /**
   * Produce a description of a node suitable for debugging.
   */
  protected static String logNode(Node node) {
    List<String> parentChain = getParentChain(node);
    String xml = recoverXml(node);
    xml = CharMatcher.WHITESPACE.trimAndCollapseFrom(xml, ' '); // Condense whitespace to log onto one line
    return String.format("{Location: %s. Node: %s}", Joiner.on('/').join(parentChain), xml);
  }

  private static List<String> getParentChain(Node node) {
    Deque<String> stack = new ArrayDeque<>();
    while (node != null) {
      stack.push(node.getNodeName());
      node = node.getParentNode();
    }
    return ImmutableList.copyOf(stack);
  }

  private static String recoverXml(Node node) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.transform(new DOMSource(node), new StreamResult(outputStream));
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
    return new String(outputStream.toByteArray()); // TODO: Encoding?
  }

}
