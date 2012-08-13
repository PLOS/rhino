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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.ambraproject.filestore.FSIDMapper;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.filestore.FileStoreService;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.util.Map;

public class XPathBatch {

  private final Map<String, String> queryStrings;

  private XPathBatch(Map<String, String> queries) {
    this.queryStrings = Preconditions.checkNotNull(queries);
  }

  public static XPathBatch empty() {
    return new XPathBatch(Maps.<String, String>newHashMap());
  }

  public static XPathBatch fromMap(Map<String, String> keyedQueries) {
    Map<String, String> queries = Maps.newHashMap(keyedQueries);
    return new XPathBatch(queries);
  }

  public Map<String, String> asMap() {
    return ImmutableMap.copyOf(queryStrings);
  }

  public Map<String, String> evaluateOnArticle(String articleDoi, FileStoreService fileStoreService) throws FileStoreException {
    String fsid = FSIDMapper.doiTofsid(articleDoi, "XML");
    if (fsid.isEmpty()) {
      throw new IllegalArgumentException("Could not parse DOI into FSID");
    }
    InputStream xmlInput = fileStoreService.getFileInStream(fsid);
    InputSource xml = new InputSource(xmlInput);

    ImmutableMap.Builder<String, String> queryResults = ImmutableMap.builder();
    XPath xPath = XPathFactory.newInstance().newXPath();
    for (Map.Entry<String, String> queryEntry : queryStrings.entrySet()) {
      String key = queryEntry.getKey();
      String query = queryEntry.getValue();

      XPathExpression xPathExpression;
      try {
        xPathExpression = xPath.compile(query);
      } catch (XPathExpressionException e) {
        throw new IllegalArgumentException("Query cannot be compiled: " + query);
      }

      String result;
      try {
         result = xPathExpression.evaluate(xml);
      } catch (XPathExpressionException e) {
        throw new IllegalArgumentException("Query cannot be evaluated: " + query);
      }

      queryResults.put(key, result);
    }
    return queryResults.build();
  }

}
