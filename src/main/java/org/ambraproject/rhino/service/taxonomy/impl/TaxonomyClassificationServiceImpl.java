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

package org.ambraproject.rhino.service.taxonomy.impl;

import static org.ambraproject.rhino.service.impl.AmbraService.newDocumentBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleCategoryAssignment;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.Category;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyClassificationService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyRemoteServiceInvalidBehaviorException;
import org.ambraproject.rhino.service.taxonomy.TaxonomyRemoteServiceNotAvailableException;
import org.ambraproject.rhino.service.taxonomy.WeightedTerm;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This is a separate bean from {@link TaxonomyServiceImpl} because it has a special dependency on the remote taxonomy
 * server, which is useful to inject separately.
 *
 * @author Alex Kudlick Date: 7/3/12
 */
@SuppressWarnings("JpaQlInspection")
public class TaxonomyClassificationServiceImpl implements TaxonomyClassificationService {

  private static final Logger log = LogManager.getLogger(TaxonomyClassificationServiceImpl.class);

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

  /* Article types to exclude from categorization. */ 
  private static final List<String> EXCLUDE_FROM_CATEGORIZATION = ImmutableList.of("correction", "expression of concern", "retraction");

  @Autowired
  private CloseableHttpClient httpClient;
  @Autowired
  private RuntimeConfiguration runtimeConfiguration;
  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  protected HibernateTemplate hibernateTemplate;

  // See https://jira.plos.org/jira/browse/AMEC-100.
  // Basically it was a one-time hack which may or may not still be
  // needed. But at some point the hack of blacklisting a single
  // category turned into a configurable feature. It seemed to me that
  // this was absolutely not something that needed to be configurable
  // at runtime, so I just moved it here. It's not clear if this is
  // still a feature we need, but we decided it was best to keep the
  // feature in for now.
  private static final String[] CATEGORY_BLACKLIST =
      new String[] {"/Earth sciences/Geography/Locations/"};
  /**
   * @inheritDoc
   */
  @Override
  public List<WeightedTerm> classifyArticle(Article article, Document articleXml)  {

    List<String> rawTerms = getRawTerms(articleXml, article, false /*isTextRequired*/);
    List<WeightedTerm> results = new ArrayList<>(rawTerms.size());

    for (String rawTerm : rawTerms) {
      WeightedTerm entry = parseVectorElement(rawTerm);
      String term = entry.getPath();
      if (term != null) {
        boolean isBlacklisted = false;
        for (String blacklistedCategory : CATEGORY_BLACKLIST) {
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

  private static final ContentType APPLICATION_XML_UTF_8 = ContentType.create("application/xml", Charsets.UTF_8);

  /**
   * @inheritDoc
   */
  @Override
  public List<String> getRawTerms(Document articleXml, Article article, boolean isTextRequired) {
    String thesaurus = runtimeConfiguration.getThesaurus();
    URI taxonomyUrl = runtimeConfiguration.getTaxonomyUrl();

    String toCategorize = getCategorizationContent(articleXml);

    ArticleIngestion latest = articleCrudService.readLatestRevision(article).getIngestion();
    String header = String.format(MESSAGE_HEADER,
        new SimpleDateFormat("yyyy-MM-dd").format(latest.getPublicationDate()),
        latest.getJournal().getTitle(),
        latest.getArticleType(),
        article.getDoi());

    String aiMessage = String.format(MESSAGE_BEGIN, thesaurus)
        + StringEscapeUtils.escapeXml10(String.format(MESSAGE_DOC_ELEMENT, header, toCategorize))
        + MESSAGE_END;

    HttpPost post = new HttpPost(taxonomyUrl.toString());
    post.setEntity(new StringEntity(aiMessage, APPLICATION_XML_UTF_8));

    DocumentBuilder documentBuilder = newDocumentBuilder();

    Document response;
    try (CloseableHttpResponse httpResponse = httpClient.execute(post);
         InputStream stream = httpResponse.getEntity().getContent()) {
      response = documentBuilder.parse(stream);
    } catch (IOException e) {
      throw new TaxonomyRemoteServiceNotAvailableException(e);
    } catch (SAXException e) {
      throw new TaxonomyRemoteServiceInvalidBehaviorException("Invalid XML returned from " + taxonomyUrl, e);
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

  @Override
  public Collection<ArticleCategoryAssignment> getAssignmentsForArticle(Article article) {
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "FROM ArticleCategoryAssignment aca " +
          "WHERE aca.article = :article");
      query.setParameter("article", article);
      return (Collection<ArticleCategoryAssignment>) query.list();
    });
  }

  private static final Splitter TAXONOMY_PATH_SPLITTER = Splitter.on('/');

  private static String getTermFromPath(String path) {
    return Iterables.getLast(TAXONOMY_PATH_SPLITTER.split(path));
  }

  @Override
  public Collection<Category> getArticleCategoriesWithTerm(Article article, String term) {
    Objects.requireNonNull(term);
    return getAssignmentsForArticle(article).stream()
        .filter((ArticleCategoryAssignment aca) -> {
          String path = aca.getCategory().getPath();
          return getTermFromPath(path).equals(term);
        })
        .map(ArticleCategoryAssignment::getCategory)
        .collect(Collectors.toList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void populateCategories(ArticleRevision revision) {
    ArticleIngestion ingestion = revision.getIngestion();

    if (!EXCLUDE_FROM_CATEGORIZATION.contains(ingestion.getArticleType().toLowerCase())) {
      Article article = ingestion.getArticle();
      Document xml = articleCrudService.getManuscriptXml(ingestion);
      List<WeightedTerm> terms = classifyArticle(article, xml);
      if (terms != null && terms.size() > 0) {
        List<WeightedTerm> leafNodes = getDistinctLeafNodes(CATEGORY_COUNT, terms);
        persistCategories(leafNodes, article);
      } else {
        log.error("Taxonomy server returned 0 terms. Cannot populate Categories. " + article.getDoi());
      }
    }
  }

  /**
   * Determine the most heavily weighted leaf nodes, then return all terms that have one of those leaf nodes.
   * <p>
   * The returned list is in descending order by weight. The order of terms with equal weight is stably preserved from
   * the input list.
   *
   * @param leafCount     the number of distinct leaf nodes to search for
   * @param weightedTerms all weighted category terms on an article
   * @return a list, in descending order by weight, of all terms whose leaf node is among the most heavily weighted
   */
  @VisibleForTesting
  static List<WeightedTerm> getDistinctLeafNodes(int leafCount, List<WeightedTerm> weightedTerms) {
    List<WeightedTerm> orderedTerms = weightedTerms.stream()
        .sorted(Comparator.comparing(WeightedTerm::getWeight).reversed())
        .collect(Collectors.toList());
    Set<String> mostWeightedLeaves = orderedTerms.stream()
        .map(WeightedTerm::getLeafTerm)
        .distinct().limit(leafCount)
        .collect(Collectors.toSet());
    return orderedTerms.stream()
        .filter(term -> mostWeightedLeaves.contains(term.getLeafTerm()))
        .collect(Collectors.toList());
  }

  @VisibleForTesting
  public void persistCategories(List<WeightedTerm> terms, Article article) {
    Set<String> termStrings = terms.stream()
        .map(WeightedTerm::getPath)
        .collect(Collectors.toSet());

    Collection<Category> existingCategories = hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM Category WHERE path IN (:terms)");
      query.setParameterList("terms", termStrings);
      return (Collection<Category>) query.list();
    });

    Map<String, Category> existingCategoryMap = Maps.uniqueIndex(existingCategories, Category::getPath);

    Collection<ArticleCategoryAssignment> existingAssignments = getAssignmentsForArticle(article);
    Map<Category, ArticleCategoryAssignment> assignmentMap = Maps.uniqueIndex(existingAssignments, ArticleCategoryAssignment::getCategory);
    assignmentMap = new HashMap<>(assignmentMap); // Make it mutable. We will remove assignments as they are updated.

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

      ArticleCategoryAssignment assignment = assignmentMap.remove(category);
      if (assignment == null) {
        hibernateTemplate.save(new ArticleCategoryAssignment(category, article, term.getWeight()));
      } else {
        assignment.setWeight(term.getWeight());
        hibernateTemplate.update(assignment);
      }
    }

    // Each assignment that was not removed from assignmentMap is not among the new terms, so it should be deleted.
    assignmentMap.values().forEach(hibernateTemplate::delete);
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
