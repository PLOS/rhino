/*
 * Copyright (c) 2006-2014 by Public Library of Science
 *
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ambraproject.rhino.service.crossref;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.service.hibernate.HibernateServiceImpl;
import org.ambraproject.service.xml.XMLServiceImpl;
import org.ambraproject.util.XPathUtil;
import org.ambraproject.xml.transform.cache.CachedSource;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.plos.crepo.service.contentRepo.ContentRepoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Query crossref for article details
 *
 * @author Joe Osowski
 */
public class CrossRefLookupServiceImpl extends HibernateServiceImpl implements CrossRefLookupService {

  private static final Logger log = LoggerFactory.getLogger(CrossRefLookupServiceImpl.class);

  private String crossRefUrl;
  private HttpClient httpClient;
  private ContentRepoService contentRepoService;

  /**
   * Store the harvested citation data
   *
   * @param identity
   * @param keyColumn
   * @param citationDOI
   */
  @Transactional
  private void setCitationDoi(final ArticleIdentity identity, final long keyColumn, final String citationDOI) {
    hibernateTemplate.execute(new HibernateCallback<Integer>() {
      @Override
      public Integer doInHibernate(Session session) throws HibernateException, SQLException {
        Query query = session.createSQLQuery("select articleID from article where doi = :doi")
            .setString("doi", identity.getKey());

        long articleID = ((BigInteger) query.uniqueResult()).longValue();

        query = session.createSQLQuery("update citedArticle set doi = :doi, lastModified = NOW()" +
            " where articleID = :articleID and keyColumn = :keyColumn")
            .setString("doi", citationDOI)
            .setLong("articleID", articleID)
            .setLong("keyColumn", keyColumn);

        int result = query.executeUpdate();
        if (result == 0) {
          log.error("Error setting articleID: {}, Key: {} to value: {}", new Object[]{articleID, keyColumn, citationDOI});
          //throw new HibernateException("No rows updated for articleID: " + articleID + " key: " + keyColumn);
        } else {
          log.debug("Set articleID: {}, Key: {} to value: {}", new Object[]{articleID, keyColumn, citationDOI});
        }

        return result;
      }
    });
  }

  private Document getArticle(ArticleIdentity identity) {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setValidating(false);

    try {
      DocumentBuilder builder = factory.newDocumentBuilder();
      EntityResolver resolver = CachedSource.getResolver(XMLServiceImpl.NLM_DTD_URL);
      builder.setEntityResolver(resolver);

      try (InputStream is = contentRepoService.getLatestRepoObjStream(identity.forXmlAsset().toString())) {
        return builder.parse(is);
      }
    } catch (IOException | ParserConfigurationException | SAXException e) {
      throw new RuntimeException("Error parsing the article xml for article " + identity, e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Transactional
  public void refreshCitedArticles(ArticleIdentity identity) {
    log.info("refreshArticleCitation for article DOI: {}", identity);

    Document article = getArticle(identity);
    List<CrossRefSearch> crossRefSearches;
    try {
      crossRefSearches = getCrossRefSearchTerms(article);
    } catch (XPathException e) {
      throw new RuntimeException(e);
    }

    for (CrossRefSearch crossRefSearch : crossRefSearches) {
      String searchTerms = crossRefSearch.buildQuery();

      if (searchTerms.length() == 0) {
        log.info("No data for citation, not searching for DOI");
      } else {
        String crossrefDoi = findDoi(searchTerms);

        if (crossrefDoi != null && !crossrefDoi.isEmpty()) {
          //A fix for FEND-1077. crossref seems to append a URL to the DOI
          crossrefDoi = crossrefDoi.replace("http://dx.doi.org/", "");

          String label = crossRefSearch.getLabel();
          long keyColumn;

          if (label != null) {
            keyColumn = Long.valueOf(label);
          } else {
            //Not able to determine value for key column, take a guess here
            //Based on the order of the element found in the XML
            //Some articles do not contain well structured XML
            keyColumn = crossRefSearch.getOriginalOrder() + 1;
          }

          log.info("refreshArticleCitation doi found: {}", crossrefDoi);
          setCitationDoi(identity, keyColumn, crossrefDoi);
        } else {
          log.info("refreshArticleCitation nothing found");
        }
      }
    }
  }

  /**
   * Generate a list of CrossRefSearch pojos from the article DOM to be used for looking up DOIs for cited articles
   *
   * @param article the article DOM
   * @return a list of pojos parsed out of the article DOM
   */
  @VisibleForTesting
  List<CrossRefSearch> getCrossRefSearchTerms(Document article) throws XPathException {
    XPathUtil xPathUtil = new XPathUtil();
    NodeList nodes = xPathUtil.selectNodes(article, ".//back/ref-list/ref");
    List<CrossRefSearch> terms = new ArrayList<>(nodes.getLength());

    for (int a = 0; a < nodes.getLength(); a++) {
      Node node = nodes.item(a);

      Node pubtypeNode = xPathUtil.selectNode(node, ".//*[@publication-type='journal']");

      if (pubtypeNode != null) {
        //Keep track of the order the elements are found in the XML (the 'a' value)
        terms.add(new CrossRefSearch(node, a));
      }
    }

    return terms;
  }


  @Override
  @Transactional(readOnly = true)
  public String findDoi(String searchString) {
    CrossRefResponse response = queryCrossRef(searchString);

    if (response != null && response.results.length > 0) {
      return response.results[0].doi;
    } else {
      return null;
    }
  }

  private CrossRefResponse queryCrossRef(String searchString) {
    PostMethod post = createCrossRefPost(searchString);

    try {
      long timestamp = System.currentTimeMillis();
      int response = httpClient.executeMethod(post);

      log.debug("Http post finished in {} ms", System.currentTimeMillis() - timestamp);

      if (response == 200) {
        String result = post.getResponseBodyAsString();
        if (result != null) {
          log.trace("JSON response received: {}", result);
          return parseJSON(result);
        }
        log.error("Received empty response, response code {}, when executing query  {}", response, crossRefUrl);
      } else {
        log.error("Received response code {} when executing query {}", response, crossRefUrl);
      }
    } catch (IOException ex) {
      log.error(ex.getMessage(), ex);
    } finally {
      // be sure the connection is released back to the connection manager
      post.releaseConnection();
    }
    return null;
  }

  /**
   * Parse the JSON into native types
   *
   * @param json the JSON string to convert to a java native type
   * @return a CrossRefResponse object
   */
  private CrossRefResponse parseJSON(final String json) {
    return new CrossRefResponse() {{
      JsonParser parser = new JsonParser();
      JsonObject responseObject = parser.parse(json).getAsJsonObject();

      queryOK = (responseObject.getAsJsonPrimitive("query_ok")).getAsBoolean();

      List<CrossRefResult> resultTemp = new ArrayList<>();

      for (final JsonElement resultElement : responseObject.getAsJsonArray("results")) {
        JsonObject resultObj = resultElement.getAsJsonObject();
        CrossRefResult res = new CrossRefResult();

        if (resultObj.getAsJsonPrimitive("text") != null) {
          res.text = resultObj.getAsJsonPrimitive("text").getAsString();
        }

        if (resultObj.getAsJsonPrimitive("match") != null) {
          res.match = resultObj.getAsJsonPrimitive("match").getAsBoolean();
        }

        if (resultObj.getAsJsonPrimitive("doi") != null) {
          res.doi = resultObj.getAsJsonPrimitive("doi").getAsString();
        }

        if (resultObj.getAsJsonPrimitive("score") != null) {
          res.score = resultObj.getAsJsonPrimitive("score").getAsString();
        }

        //Some results aren't actually valid
        if (res.doi != null) {
          resultTemp.add(res);
        }
      }

      this.results = resultTemp.toArray(new CrossRefResult[resultTemp.size()]);
    }};
  }

  private PostMethod createCrossRefPost(String searchString) {
    //Example query to post:
    //["Young GC,Analytical methods in palaeobiogeography, and the role of early vertebrate studies;Palaeoworld;19;160-173"]

    //Use toJSON to encode strings with proper escaping
    final String json = "[" + (new Gson()).toJson(searchString) + "]";

    if (this.crossRefUrl == null) {
      throw new RuntimeException("ambra.services.crossref.query.url value not found in configuration.");
    }

    return new PostMethod(this.crossRefUrl) {{
      addRequestHeader("Content-Type", "application/json");
      setRequestEntity(new RequestEntity() {
        @Override
        public boolean isRepeatable() {
          return false;
        }

        @Override
        public void writeRequest(OutputStream outputStream) throws IOException {
          outputStream.write(json.getBytes());
        }

        @Override
        public long getContentLength() {
          return json.getBytes().length;
        }

        @Override
        public String getContentType() {
          return "application/json";
        }
      });
    }};
  }

  /* utility class for internally tracking data */
  private class CrossRefResult {
    public String text;
    public Boolean match;
    public String doi;
    public String score;
  }

  /* utility class for internally tracking data */
  private class CrossRefResponse {
    public CrossRefResult[] results;
    public Boolean queryOK;
  }

  @Required
  public void setHttpClient(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Required
  public void setCrossRefUrl(String crossRefUrl) {
    this.crossRefUrl = crossRefUrl;
  }

  @Required
  public void setContentRepoService(ContentRepoService contentRepoService) {
    this.contentRepoService = contentRepoService;
  }
}


