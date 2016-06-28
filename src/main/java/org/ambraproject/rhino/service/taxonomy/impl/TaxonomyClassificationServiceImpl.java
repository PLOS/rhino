package org.ambraproject.rhino.service.taxonomy.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.Category;
import org.ambraproject.rhino.model.WeightedCategory;
import org.ambraproject.rhino.model.WeightedCategoryId;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleTypeService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyClassificationService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyRemoteServiceInvalidBehaviorException;
import org.ambraproject.rhino.service.taxonomy.TaxonomyRemoteServiceNotAvailableException;
import org.ambraproject.rhino.service.taxonomy.TaxonomyRemoteServiceNotConfiguredException;
import org.ambraproject.rhino.service.taxonomy.WeightedTerm;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.hibernate.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.ambraproject.rhino.service.impl.AmbraService.newDocumentBuilder;

/**
 * This is a separate bean from {@link TaxonomyServiceImpl} because it has a special dependency on the remote taxonomy
 * server, which is useful to inject separately.
 *
 * @author Alex Kudlick Date: 7/3/12
 */
public class TaxonomyClassificationServiceImpl implements TaxonomyClassificationService {

  private static final Logger log = LoggerFactory.getLogger(TaxonomyClassificationServiceImpl.class);

  private static final String MESSAGE_BEGIN = "<TMMAI project='%s' location = '.'>\n" +
      "  <Method name='getSuggestedTermsFullPathsPlos' returnType='java.util.Vector'/>\n" +
      "  <VectorParam>\n" +
      "    <VectorElement>\n";

  private static final String MESSAGE_DOC_ELEMENT=
      "      <doc>\n" +
      "        <header>\n" +
      "%s" +
      "        </header>\n" +
      "        <content>\n" +
      "          %s\n" +
      "        </content>\n" +
      "      </doc>\n";

  private static final String MESSAGE_HEADER =
      "          <publication-date>%s</publication-date>\n" +
      "          <journal-title>%s</journal-title>\n" +
      "          <article-type>%s</article-type>\n" +
      "          <article-id pub-id-type=\"doi\">%s</article-id>\n";

  private static final String MESSAGE_END =
      "    </VectorElement>\n" +
      "  </VectorParam>\n" +
      "</TMMAI>";

  // Number of most-weighted category leaf nodes to associate with each article
  // TODO: Make configurable?
  private static final int CATEGORY_COUNT = 8;

  @Autowired
  private CloseableHttpClient httpClient;
  @Autowired
  private RuntimeConfiguration runtimeConfiguration;
  @Autowired
  private ArticleTypeService articleTypeService;
  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  protected HibernateTemplate hibernateTemplate;

  /**
   * @inheritDoc
   */
  @Override
  public List<WeightedTerm> classifyArticle(ArticleTable article, Document articleXml)  {
    RuntimeConfiguration.TaxonomyConfiguration configuration = getTaxonomyConfiguration();

    List<String> rawTerms = getRawTerms(articleXml, article, false /*isTextRequired*/);
    List<WeightedTerm> results = new ArrayList<>(rawTerms.size());

    for (String rawTerm : rawTerms) {
      WeightedTerm entry = parseVectorElement(rawTerm);
      String term = entry.getPath();
      if (term != null) {
        boolean isBlacklisted = false;
        for (String blacklistedCategory : configuration.getCategoryBlacklist()) {
          if (term.startsWith(blacklistedCategory)) {
            isBlacklisted = true;
            break;
          }
        }
        if (!isBlacklisted) {
          results.add(entry);
        }
      }
    }
    return results;
  }

  private RuntimeConfiguration.TaxonomyConfiguration getTaxonomyConfiguration() {
    RuntimeConfiguration.TaxonomyConfiguration configuration = runtimeConfiguration.getTaxonomyConfiguration();
    if (configuration.getServer() == null || configuration.getThesaurus() == null) {
      throw new TaxonomyRemoteServiceNotConfiguredException();
    }
    return configuration;
  }

  private static final ContentType APPLICATION_XML_UTF_8 = ContentType.create("application/xml", Charsets.UTF_8);

  /**
   * @inheritDoc
   */
  @Override
  public List<String> getRawTerms(Document articleXml, ArticleTable article, boolean isTextRequired) {
    RuntimeConfiguration.TaxonomyConfiguration configuration = getTaxonomyConfiguration();


    String toCategorize = getCategorizationContent(articleXml);

    String header = String.format(MESSAGE_HEADER,
        new SimpleDateFormat("yyyy-MM-dd").format(article.getPublicationDate()),
        articleCrudService.getPublicationJournal(article).getTitle(),
        "TODO",//articleTypeService.getArticleType(article).getHeading(), //todo: add article type here
        article.getDoi());

    String aiMessage = String.format(MESSAGE_BEGIN, configuration.getThesaurus())
        + StringEscapeUtils.escapeXml10(String.format(MESSAGE_DOC_ELEMENT, header, toCategorize))
        + MESSAGE_END;

    HttpPost post = new HttpPost(configuration.getServer().toString());
    post.setEntity(new StringEntity(aiMessage, APPLICATION_XML_UTF_8));

    DocumentBuilder documentBuilder = newDocumentBuilder();

    Document response;
    try (CloseableHttpResponse httpResponse = httpClient.execute(post);
         InputStream stream = httpResponse.getEntity().getContent()) {
      response = documentBuilder.parse(stream);
    } catch (IOException e) {
      throw new TaxonomyRemoteServiceNotAvailableException(e);
    } catch (SAXException e) {
      throw new TaxonomyRemoteServiceInvalidBehaviorException("Invalid XML returned from " + configuration.getServer(), e);
    }

    //parse result
    NodeList vectorElements = response.getElementsByTagName("VectorElement");
    List<String> results = new ArrayList<>(vectorElements.getLength());

    // Add the text that is sent to taxonomy server if isTextRequired is true
    if (isTextRequired) {
      toCategorize = StringEscapeUtils.unescapeXml(toCategorize);
      results.add(toCategorize);
    }

    //The first and last elements of the vector response are just MAITERMS
    for (int i = 1; i < vectorElements.getLength() - 1; i++) {
      results.add(vectorElements.item(i).getTextContent());
    }

    if ((isTextRequired && results.size() == 1) || results.isEmpty()) {
      log.error("Taxonomy server returned 0 terms. " + article.getDoi());
    }

    return results;
  }

  /**
   * Populates article category information by making a call to the taxonomy server. Will not throw
   * an exception if we cannot communicate or get results from the taxonomy server. Will not
   * request weightedTerms for amendments.
   *
   * @param article the Article model instance
   * @param xml     Document representing the article XML
   */
  public void populateCategories(ArticleTable article, Document xml) {
    List<WeightedTerm> terms;
    String doi = article.getDoi();

    article.setCategories(new HashSet<>());
    boolean isAmendment = false; //todo: fix or remove this when we find a home for article types

    if (!isAmendment) {
      terms = classifyArticle(article, xml);
      if (terms != null && terms.size() > 0) {
        setArticleCategories(article, terms);
      } else {
        log.error("Taxonomy server returned 0 terms. Cannot populate Categories. " + doi);
      }
    }
  }

  private void setArticleCategories(ArticleTable article, List<WeightedTerm> weightedTerms) {
    weightedTerms = WeightedTerm.BY_DESCENDING_WEIGHT.immutableSortedCopy(weightedTerms);

    List<WeightedTerm> results = new ArrayList<>(weightedTerms.size());
    Set<String> uniqueLeafs = new HashSet<>();

    for (WeightedTerm s : weightedTerms) {
      if (s.getPath().charAt(0) != '/') {
        throw new IllegalArgumentException("Bad category: " + s);
      }

      //We want a count of distinct lead nodes.  When this
      //Reaches eight stop.  Note the second check, we can be at
      //eight uniqueLeafs, but still finding different paths.  Stop
      //Adding when a new unique leaf is found.  Yes, a little confusing
      if (uniqueLeafs.size() == CATEGORY_COUNT && !uniqueLeafs.contains(s.getLeafTerm())) {
        break;
      } else {
        //getSubCategory returns leaf node of the path
        uniqueLeafs.add(s.getLeafTerm());
        results.add(s);
      }
    }

    article.setCategories(resolveIntoCategories(results, article));
  }

  private Set<Category> resolveIntoCategories(List<WeightedTerm> terms, ArticleTable article) {
    Set<String> termStrings = terms.stream()
        .map(WeightedTerm::getPath)
        .collect(Collectors.toSet());

    Collection<Category> existingCategories = hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM Category WHERE path IN (:terms)");
      query.setParameterList("terms", termStrings);
      return query.list();
    });
    Map<String, Category> existingCategoryMap = Maps.uniqueIndex(existingCategories, Category::getPath);

    Set<Category> categories = new HashSet<>();
    for (WeightedTerm term : terms) {
      Category category = existingCategoryMap.get(term.getPath());
      if (category == null) {
        /*
         * A new category from the taxonomy server, which is not yet persisted in our system. Create it now.
         *
         * This risks a race condition if two articles are being populated concurrently and both have the same new
         * category, which can cause a "MySQLIntegrityConstraintViolationException: Duplicate entry" error.
         */
        category = new Category();
        category.setPath(term.getPath());
        hibernateTemplate.save(category);
      }
      WeightedCategoryId id = new WeightedCategoryId(category.getCategoryId(), article.getArticleId().intValue());
      WeightedCategory weightedCategory = new WeightedCategory(id, term.getWeight());
      hibernateTemplate.save(weightedCategory);
      categories.add(category);
    }
    return categories;
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
   * @return the term and weight of the term
   */
  @VisibleForTesting
  static WeightedTerm parseVectorElement(String vectorElement) {
    Matcher match = TERM_PATTERN.matcher(vectorElement);

    if (match.find()) {
      String text = match.group(1);
      int value = Integer.parseInt(match.group(2));

      return new WeightedTerm(text, value);
    } else {
      //Bad term
      throw new TaxonomyRemoteServiceInvalidBehaviorException("Invalid syntax: " + vectorElement);
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
   * text. This is usually the case for non-research-articles, such as corrections, opinion pieces, etc.
   * Please not that the "getSuggestedTermsFullPathsPlos" requires the data within the "content" tag to be
   * XML-escaped twice. Hence, we XML escape it once in this method and once when we escape the "doc" tag in
   * {@link getRawTerms} method.
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
    return StringEscapeUtils.escapeXml10(sb.toString().trim());
  }

}
