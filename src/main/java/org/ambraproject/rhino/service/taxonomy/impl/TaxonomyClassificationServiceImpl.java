package org.ambraproject.rhino.service.taxonomy.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.service.taxonomy.TaxonomyClassificationService;
import org.ambraproject.util.DocumentBuilderFactoryCreator;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a separate bean from {@link TaxonomyServiceImpl} because it has a special dependency on the remote taxonomy
 * server, which is useful to inject separately.
 *
 * @author Alex Kudlick Date: 7/3/12
 */
public class TaxonomyClassificationServiceImpl implements TaxonomyClassificationService {

  private static final Logger log = LoggerFactory.getLogger(TaxonomyClassificationServiceImpl.class);

  private static final String MESSAGE_BEGIN = "<TMMAI project='%s' location = '.'>\n" +
      "  <Method name='getSuggestedTermsFullPaths' returnType='java.util.Vector'/>\n" +
      "  <VectorParam>\n" +
      "    <VectorElement>";

  private static final String MESSAGE_END = "</VectorElement>\n" +
      "  </VectorParam>\n" +
      "</TMMAI>";


  @Autowired
  private CloseableHttpClient httpClient;
  @Autowired
  private RuntimeConfiguration runtimeConfiguration;

  /**
   * @inheritDoc
   */
  @Override
  public Map<String, Integer> classifyArticle(Document articleXml) throws IOException {
    RuntimeConfiguration.TaxonomyConfiguration configuration = runtimeConfiguration.getTaxonomyConfiguration();
    if (configuration.getServer() == null || configuration.getThesaurus() == null) {
      throw new TaxonomyClassificationServiceNotConfiguredException();
    }

    List<String> rawTerms = getRawTerms(articleXml);
    Map<String, Integer> results = new LinkedHashMap<>(rawTerms.size());

    for (String rawTerm : rawTerms) {
      Map.Entry<String, Integer> entry = parseVectorElement(rawTerm);
      String key = entry.getKey();
      if (key != null) {
        boolean isBlacklisted = false;
        for (String blacklistedCategory : configuration.getCategoryBlacklist()) {
          if (key.startsWith(blacklistedCategory)) {
            isBlacklisted = true;
            break;
          }
        }
        if (!isBlacklisted) {
          results.put(key, entry.getValue());
        }
      }
    }
    return results;
  }

  /**
   * Queries the MAI server for taxonomic terms for a given article, and returns a list of the raw results.
   *
   * @param articleXml DOM of the article to categorize
   * @return List of results from the server.  This will consist of raw XML fragments, and include things like counts
   * that we don't currently store in mysql.
   * @throws IOException
   */
  private List<String> getRawTerms(Document articleXml) throws IOException {
    RuntimeConfiguration.TaxonomyConfiguration configuration = runtimeConfiguration.getTaxonomyConfiguration();
    String toCategorize = getCategorizationContent(articleXml);
    String aiMessage = String.format(MESSAGE_BEGIN, configuration.getThesaurus()) + toCategorize + MESSAGE_END;
    HttpPost post = new HttpPost(configuration.getServer().toString());
    post.setEntity(new StringEntity(aiMessage, ContentType.APPLICATION_XML));

    DocumentBuilder documentBuilder;
    try {
      documentBuilder = DocumentBuilderFactoryCreator.createFactory().newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e); // parser configuration is default; expected never to happen
    }

    Document response;
    try (CloseableHttpResponse httpResponse = httpClient.execute(post);
         InputStream stream = httpResponse.getEntity().getContent()) {
      response = documentBuilder.parse(stream);
    } catch (SAXException e) {
      throw new RuntimeException("Invalid XML returned from " + configuration.getServer(), e);
    }

    //parse result
    NodeList vectorElements = response.getElementsByTagName("VectorElement");
    List<String> results = new ArrayList<>(vectorElements.getLength());
    //The first and last elements of the vector response are just MAITERMS
    for (int i = 1; i < vectorElements.getLength() - 1; i++) {
      results.add(vectorElements.item(i).getTextContent());
    }
    return results;
  }


  // There appears to be a bug in the AI getSuggestedTermsFullPath method.
  // It's supposed to return a slash-delimited path that starts with a slash,
  // like an absolute Unix file path.  However, rarely, it just returns "naked"
  // terms without the leading slash.  Discard these, since the calling
  // code won't be able to handle this.  (Note the first slash after <TERM> in the regex)

  //Positive (Good term) example response:
  //"<TERM>/Biology and life sciences/Computational biology/Computational neuroscience/Single neuron function|(5) neuron*(5)</TERM>"
  //This regex:
  //Confirms the response is good
  //Finds the term and places in the result
  //Finds first number wrapped in parentheses after the pipe symbol and places it in the result
  private static final Pattern TERM_PATTERN = Pattern.compile("<TERM>\\s*(/.*)\\|\\s*\\((\\d+)\\).*</TERM>");

  /**
   * Parses a single line of the XML response from the taxonomy server.
   *
   * @param vectorElement The text body of a line of the response
   * @return the term and weight of the term or null if the line is not valid
   */
  @VisibleForTesting
  static Map.Entry<String, Integer> parseVectorElement(String vectorElement) {
    Matcher match = TERM_PATTERN.matcher(vectorElement);

    if (match.find()) {
      String text = match.group(1);
      Integer value = Integer.valueOf(match.group(2));

      return new AbstractMap.SimpleImmutableEntry<>(text, value);
    } else {
      //Bad term, return null
      return null;
    }
  }

  /**
   * Adds the text content of the given element to the StringBuilder, if it exists. If more than one element exists with
   * the given name, only appends the first one.
   *
   * @param sb          StringBuilder to be modified
   * @param dom         DOM tree of an article
   * @param elementName name of element to search for in the dom
   * @return true if the StringBuilder was modified
   */
  @VisibleForTesting
  static boolean appendElementIfExists(StringBuilder sb, Document dom, String elementName) {
    NodeList list = dom.getElementsByTagName(elementName);
    if (list != null && list.getLength() > 0) {
      sb.append(list.item(0).getTextContent());
      sb.append("\n");
      return true;
    } else {
      return false;
    }
  }

  /**
   * Adds the text content of all found elements to the StringBuilder, if they exist.
   *
   * @param sb          StringBuilder to be modified
   * @param dom         DOM tree of an article
   * @param elementName name of element to search for in the dom
   * @return true if the StringBuilder was modified
   */
  private static boolean appendAllElementsIfExists(StringBuilder sb, Document dom, String elementName) {
    NodeList list = dom.getElementsByTagName(elementName);
    if (list != null && list.getLength() > 0) {
      for (int a = 0; a < list.getLength(); a++) {
        sb.append(list.item(a).getTextContent());
        sb.append("\n");
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * Returns a string containing only the parts of the article that should be examined by the taxonomy server.  For
   * research articles, this is presently the title, the abstract, the Materials and Methods section, and the Results
   * section.  (If any of these sections are not present, they are not sent, but this is not a fatal error.) If none of
   * these sections (abstract, materials/methods, or results) are present, then this method will return the entire body
   * text.  This is usually the case for non-research-articles, such as corrections, opinion pieces, etc.
   *
   * @param dom DOM tree of an article
   * @return raw text content, XML-escaped, of the relevant article sections
   */
  @VisibleForTesting
  static String getCategorizationContent(Document dom) {
    StringBuilder sb = new StringBuilder();
    appendElementIfExists(sb, dom, "article-title");
    appendAllElementsIfExists(sb, dom, "abstract");
    appendElementIfExists(sb, dom, "body");
    return StringEscapeUtils.escapeXml(sb.toString().trim());
  }

}
