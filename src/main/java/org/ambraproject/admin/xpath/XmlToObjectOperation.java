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

package org.ambraproject.admin.xpath;

import com.google.common.base.Preconditions;
import org.ambraproject.admin.util.NodeListAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.List;

/**
 * An XPath query meant to be applied in bulk to modify an object.
 *
 * @param <T> the object type to modify
 * @param <V> the type of value that this instance reads from XML
 */
public abstract class XmlToObjectOperation<T, V> {

  private static final Logger log = LoggerFactory.getLogger(XmlToObjectOperation.class);

  protected final String xPathQuery;

  protected XmlToObjectOperation(String xPathQuery) {
    this.xPathQuery = Preconditions.checkNotNull(xPathQuery);
  }


  /**
   * Produce a new XPath object.
   * <p/>
   * Must use a new {@link XPathFactory} object every time this method is called, because it is not thread-safe.
   *
   * @return a new XPath object
   */
  protected XPath getXPath() {
    return XPathFactory.newInstance().newXPath();
  }

  /**
   * Compile the expression for this instance.
   * <p/>
   * Must return a new expression object every time this method is called, because {@link XPathExpression} is not
   * thread-safe.
   *
   * @return a new {@link XPathExpression} object compiled from this object's query
   */
  protected XPathExpression getExpression() {
    try {
      return getXPath().compile(xPathQuery);
    } catch (XPathExpressionException e) {
      // Queries are expected to be programmer-defined constants; this shouldn't be possible from user error
      throw new RuntimeException("Could not compile: " + xPathQuery);
    }
  }

  protected List<Node> queryForNodeList(String query, Node node) throws XPathExpressionException {
    NodeList result = (NodeList) getXPath().evaluate(query, node, XPathConstants.NODESET);
    return NodeListAdapter.wrap(result);
  }

  /**
   * Modify an object by setting a value.
   *
   * @param obj   the object to modify
   * @param value the value to set
   */
  protected abstract void apply(T obj, V value) throws XPathExpressionException, XmlContentException;

  /**
   * Read a value from XML.
   *
   * @param xml the XML structure to read (typically a full document)
   * @return the found value
   * @throws XPathExpressionException if this object's XPath query cannot be applied to the supplied XML structure
   */
  protected abstract V extract(Node xml) throws XPathExpressionException;

  /**
   * Read the value corresponding to this instance from an XML document and put that value into an object.
   *
   * @param obj the object to modify with the found value
   * @param xml the XML structure from which to retrieve the value
   * @throws XPathExpressionException if this object's XPath query cannot be applied to the supplied XML structure
   */
  public final void evaluate(T obj, Node xml) throws XPathExpressionException, XmlContentException {
    V value = extract(xml);
    if (value != null) {
      apply(obj, value);
    } else {
      log.debug("Nothing found for {}", xPathQuery);
    }
  }


  protected static abstract class StringExpression<T> extends XmlToObjectOperation<T, String> {
    protected StringExpression(String xPathQuery) {
      super(xPathQuery);
    }

    @Override
    protected String extract(Node xml) throws XPathExpressionException {
      Node stringNode = (Node) getExpression().evaluate(xml, XPathConstants.NODE);
      return stringNode.getTextContent();
    }
  }

  protected static abstract class NodeExpression<T> extends XmlToObjectOperation<T, Node> {
    protected NodeExpression(String xPathQuery) {
      super(xPathQuery);
    }

    @Override
    protected Node extract(Node xml) throws XPathExpressionException {
      return (Node) getExpression().evaluate(xml, XPathConstants.NODE);
    }
  }

  protected static abstract class NodeListExpression<T> extends XmlToObjectOperation<T, List<Node>> {
    protected NodeListExpression(String xPathQuery) {
      super(xPathQuery);
    }

    @Override
    protected List<Node> extract(Node xml) throws XPathExpressionException {
      return queryForNodeList(xPathQuery, xml);
    }
  }


  @Override
  public String toString() {
    return xPathQuery;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    return xPathQuery.equals(((XmlToObjectOperation) o).xPathQuery);
  }

  @Override
  public int hashCode() {
    return xPathQuery.hashCode();
  }

}
