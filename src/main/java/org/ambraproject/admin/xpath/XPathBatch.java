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

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.ambraproject.filestore.FSIDMapper;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.filestore.FileStoreService;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XPathBatch {

  private final ImmutableMap<String, String> queries;
  private final ImmutableMap<String, XPathExpression> expressions;

  private XPathBatch(Map<String, String> queries) {
    this.queries = ImmutableMap.copyOf(queries); // defensive copy
    this.expressions = compileExpressions();
  }

  private ImmutableMap<String, XPathExpression> compileExpressions() {
    XPath xPath = XPathFactory.newInstance().newXPath();
    ImmutableMap.Builder<String, XPathExpression> compiled = ImmutableMap.builder();
    for (Map.Entry<String, String> queryEntry : queries.entrySet()) {
      String key = queryEntry.getKey();
      String query = queryEntry.getValue();

      XPathExpression xPathExpression = null;
      try {
        xPathExpression = xPath.compile(query);
      } catch (XPathExpressionException e) {
        throw new IllegalArgumentException("Can't compile XPath query: " + query);
      }
      compiled.put(key, xPathExpression);
    }
    return compiled.build();
  }

  /**
   * Factory method.
   *
   * @param keyedQueries a set of XPath queries (the map values) with arbitrary labels (the map keys)
   * @return an instance
   */
  public static XPathBatch fromMap(Map<String, String> keyedQueries) {
    return new XPathBatch(keyedQueries);
  }

  public static XPathBatch useQueriesAsKeys(Iterable<String> queries) {
    Map<String, String> queriesAsKeys = Maps.uniqueIndex(queries, Functions.<String>identity());
    return XPathBatch.fromMap(queriesAsKeys);
  }

  private static final Pattern INGEST_QUERY_STRUCTURE = Pattern.compile(
      "//(\\p{Alnum}+/)*(\\p{Alnum}+)(/\\p{Alnum}+\\(\\))?");

  /*
   * TODO Is this inapplicable except to transformed XML (which we won't make)?
   */
  public static XPathBatch inferKeysFromIngestQueries(Iterable<String> queries) {
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    for (String query : queries) {
      Matcher m = INGEST_QUERY_STRUCTURE.matcher(query);
      if (!m.matches()) {
        throw new IllegalArgumentException("Cannot infer key from structure of query");
      }
      String inferredKey = m.group(2);
      builder.put(inferredKey, query);
    }
    return XPathBatch.fromMap(builder.build());
  }

  /**
   * Get the queries that produced this object.
   *
   * @return a set of XPath queries, keyed by their names
   */
  public Map<String, String> asQueryMap() {
    return queries;
  }

  /**
   * Perform this batch of queries on an article's XML file.
   * <p/>
   * The returned map has a key set equal to that of the query map used to build this object. The map's values are the
   * results of each query when applied to the article XML.
   *
   * @param articleDoi       the DOI of the article
   * @param fileStoreService a file store service for the file store containing the article
   * @return the map of results
   * @throws XPathExpressionException if the article cannot have one or more of this object's queries evaluated on it
   */
  public Map<String, String> evaluateOnArticle(String articleDoi, FileStoreService fileStoreService) throws XPathExpressionException {
    Preconditions.checkNotNull(articleDoi);
    Preconditions.checkNotNull(fileStoreService);
    String fsid = FSIDMapper.doiTofsid(articleDoi, "XML");
    if (fsid.isEmpty()) {
      throw new IllegalArgumentException("Could not parse DOI into FSID" + articleDoi);
    }

    InputStream xmlInput = null;
    try {
      xmlInput = fileStoreService.getFileInStream(fsid);
      return evaluate(new InputSource(xmlInput));
    } catch (FileStoreException e) {
      throw new IllegalArgumentException("DOI not found in file store: " + articleDoi, e);
    } finally {
      IOUtils.closeQuietly(xmlInput);
    }
  }

  public Map<String, String> evaluate(Document xml) throws XPathExpressionException {
    return evaluate((Object) xml);
  }

  /**
   * Apply this object's queries to XML.
   * <p/>
   * The argument's runtime type must be either {@link InputSource} or another type accepted by {@link
   * XPathExpression#evaluate(java.lang.Object)} (for example, any class that implements {@link org.w3c.dom.Node}, such
   * as {@link Document}). This weirdness is inherited from the API of {@link XPathExpression}.
   *
   * @param xml the XML object to parse
   * @return the map of results
   * @throws XPathExpressionException
   */
  private Map<String, String> evaluate(Object xml) throws XPathExpressionException {
    ImmutableMap.Builder<String, String> results = ImmutableMap.builder();
    for (Map.Entry<String, XPathExpression> expressionEntry : expressions.entrySet()) {
      String key = expressionEntry.getKey();
      XPathExpression expression = expressionEntry.getValue();

      String result = (xml instanceof InputSource) ? expression.evaluate((InputSource) xml) : expression.evaluate(xml);
      results.put(key, result);
    }
    return results.build();
  }

}
