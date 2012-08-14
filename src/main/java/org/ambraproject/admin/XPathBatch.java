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

package org.ambraproject.admin;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.ambraproject.filestore.FSIDMapper;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.filestore.FileStoreService;
import org.apache.commons.io.IOUtils;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.util.Map;

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
   * @throws IllegalArgumentException if the article cannot have all of this object's queries evaluated on it
   */
  public Map<String, String> evaluateOnArticle(String articleDoi, FileStoreService fileStoreService) {
    Preconditions.checkNotNull(articleDoi);
    String fsid = FSIDMapper.doiTofsid(articleDoi, "XML");
    if (fsid.isEmpty()) {
      throw new IllegalArgumentException("Could not parse DOI into FSID" + articleDoi);
    }

    InputStream xmlInput = null;
    try {
      xmlInput = fileStoreService.getFileInStream(fsid);
      return evaluate(xmlInput, fileStoreService);
    } catch (FileStoreException e) {
      throw new IllegalArgumentException("DOI not found in file store: " + articleDoi, e);
    } finally {
      IOUtils.closeQuietly(xmlInput);
    }
  }

  private Map<String, String> evaluate(InputStream input, FileStoreService fileStoreService) {
    Preconditions.checkNotNull(fileStoreService);
    InputSource xml = new InputSource(input);

    ImmutableMap.Builder<String, String> results = ImmutableMap.builder();
    for (Map.Entry<String, XPathExpression> expressionEntry : expressions.entrySet()) {
      String key = expressionEntry.getKey();
      XPathExpression expression = expressionEntry.getValue();

      String result;
      try {
        result = expression.evaluate(xml);
      } catch (XPathExpressionException e) {
        throw new IllegalArgumentException("Query cannot be evaluated: " + queries.get(key), e);
      }
      results.put(key, result);
    }
    return results.build();
  }

}
