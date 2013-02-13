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

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.ambraproject.rhino.util.NodeListAdapter;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.List;

/**
 * A container for a node of XML data that reads it with XPath queries.
 * <p/>
 * Instances of this class are not thread-safe because they hold an instance of {@link XPath} to use.
 */
public abstract class XpathReader {

  private final Node xml;
  private final XPath xPath;

  protected XpathReader(Node xml) {
    this.xml = Preconditions.checkNotNull(xml);

    // XPath isn't thread-safe, so we need one per instance of this class
    this.xPath = XPathFactory.newInstance().newXPath();
  }

  protected String readString(String query) {
    return readString(query, xml);
  }

  protected String readString(String query, Node node) {
    Node stringNode = readNode(query, node);
    if (stringNode == null) {
      return null;
    }
    String text = stringNode.getTextContent();
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

  private static final Function<Node, String> GET_TEXT_CONTENT = new Function<Node, String>() {
    @Override
    public String apply(Node input) {
      return input.getTextContent();
    }
  };

  protected List<String> readTextList(String query) {
    return Lists.transform(readNodeList(query), GET_TEXT_CONTENT);
  }

}