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

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.ambraproject.admin.util.PersonName;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAuthor;
import org.ambraproject.models.ArticleEditor;
import org.ambraproject.models.Category;
import org.ambraproject.models.CitedArticle;
import org.ambraproject.models.CitedArticleAuthor;
import org.ambraproject.models.CitedArticleEditor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

/**
 * A set of operations for translating an article XML into an {@link Article} object.
 */
public class XmlToArticle {

  private static final Logger log = LoggerFactory.getLogger(XmlToArticle.class);
  private static final String DOI_PREFIX = "info:doi/";

  private XmlToArticle() {
    throw new RuntimeException("Not instantiable");
  }


  public static void evaluate(Article article, Document xml) throws XPathExpressionException, XmlContentException {
    setConstants(article);
    for (XmlToObjectOperation<Article, ?> field : FIELDS) {
      field.evaluate(article, xml);
    }
  }

  /**
   * Set constant values.
   * <p/>
   * Prior to this version, these values were hard-coded either in the XSLT files that did the job of this class or in
   * the Java class (XslIngestArchiveProcessor) that read them.
   *
   * @param article the article to modify
   */
  private static void setConstants(Article article) {
    // These are fine because they are implied by how we get the input
    article.setFormat("text/xml");
    article.setState(Article.STATE_UNPUBLISHED);

    /*
     * In current usage, we expect input always to be in English, but this is so purpose-specific that it ought not to
     * be hard-coded. Possibly refactor to extract from the XML (from {@code "/article@xml:lang"} perhaps).
     */
    article.setLanguage("en");
  }

  @SuppressWarnings("unchecked") // can't parameterize array
  private static final ImmutableCollection<XmlToObjectOperation<Article, ?>> FIELDS
      = ImmutableList.<XmlToObjectOperation<Article, ?>>copyOf(new XmlToObjectOperation[]{

      new XmlToObjectOperation.StringExpression<Article>(
          "/article/front/article-meta/article-id[@pub-id-type=\"doi\"]") {
        @Override
        protected void apply(Article article, String value) {
          String doiAccordingToRest = article.getDoi();
          String doiAccordingToXml = DOI_PREFIX + value;
          if (!doiAccordingToRest.equals(doiAccordingToXml)) {
            if (log.isWarnEnabled()) {
              log.warn("Article at DOI=" + doiAccordingToRest + " has XML listing DOI as " + doiAccordingToXml);
            }
          }
        }
      },

      new XmlToObjectOperation.StringExpression<Article>(
          "/article/front/article-meta/title-group/article-title") {
        @Override
        protected void apply(Article obj, String value) {
          obj.setTitle(value);
        }
      },

      new XmlToObjectOperation.StringExpression<Article>(
          "/article/front/journal-meta/issn[@pub-type=\"epub\"]") {
        @Override
        protected void apply(Article obj, String value) {
          obj.seteIssn(value);
        }
      },

      new XmlToObjectOperation.StringExpression<Article>(
          "/article/front/article-meta/abstract") {
        @Override
        protected void apply(Article article, String value) {
          article.setDescription(value);
        }
      },

      new XmlToObjectOperation.StringExpression<Article>(
          "/article/front/article-meta/copyright-statement") {
        @Override
        protected void apply(Article article, String value) {
          article.setRights(value);
        }
      },

      new XmlToObjectOperation.NodeExpression<Article>(
          "/article/front/article-meta/pub-date[@pub-type=\"epub\"]") {
        @Override
        protected void apply(Article obj, Node value) throws XPathExpressionException, XmlContentException {
          XPath xPath = getXPath();
          int year, month, day;
          try {
            year = Integer.parseInt(xPath.evaluate("year", value));
            month = Integer.parseInt(xPath.evaluate("month", value));
            day = Integer.parseInt(xPath.evaluate("day", value));
          } catch (NumberFormatException e) {
            throw new XmlContentException("Expected numbers for date fields", e);
          }

          // TODO Avoid setting to system-default time zone and locale
          Calendar date = GregorianCalendar.getInstance();
          date.set(year, month, day, 0, 0, 0); // TODO Is this the correct convention?
          obj.setDate(date.getTime());
        }
      },

      new XmlToObjectOperation.StringExpression<Article>(
          "/article/front/article-meta/elocation-id") {
        @Override
        protected void apply(Article obj, String value) {
          obj.seteLocationId(value);
        }
      },

      new XmlToObjectOperation.StringExpression<Article>(
          "/article/front/article-meta/volume") {
        @Override
        protected void apply(Article obj, String value) {
          obj.setVolume(value);
        }
      },

      new XmlToObjectOperation.StringExpression<Article>(
          "/article/front/article-meta/issue") {
        @Override
        protected void apply(Article obj, String value) {
          obj.setIssue(value);
        }
      },

      new XmlToObjectOperation.StringExpression<Article>(
          "/article/front/journal-meta/journal-title") {
        @Override
        protected void apply(Article obj, String value) {
          obj.setJournal(value);
        }
      },

      new XmlToObjectOperation.StringExpression<Article>(
          "/article/front/journal-meta/publisher/publisher-name") {
        @Override
        protected void apply(Article obj, String value) {
          obj.setPublisherName(value);
        }
      },

      new XmlToObjectOperation.StringExpression<Article>(
          "/article/front/journal-meta/publisher/publisher-loc") {
        @Override
        protected void apply(Article obj, String value) {
          obj.setPublisherLocation(value);
        }
      },

      new XmlToObjectOperation.NodeListExpression<Article>(
          "/article/@article-type") {
        @Override
        protected void apply(Article obj, List<Node> value) {
          Set<String> articleTypes = Sets.newHashSetWithExpectedSize(value.size());
          for (Node node : value) {
            articleTypes.add(node.getTextContent());
          }
          obj.setTypes(articleTypes);
        }
      },

      new XmlToObjectOperation.NodeListExpression<Article>(
          "/article/front/article-meta/article-categories/subj-group[@subj-group-type=\"Discipline\"]/subject") {
        @Override
        protected void apply(Article obj, List<Node> value) {
          Set<Category> categories = Sets.newHashSetWithExpectedSize(value.size());
          for (Node node : value) {
            categories.add(parseCategory(node.getTextContent()));
          }
          obj.setCategories(categories);
        }
      },

      new XmlToObjectOperation.NodeListExpression<Article>(
          "/article/front/article-meta/contrib-group/contrib[@contrib-type=\"author\"]/collab") {
        @Override
        protected void apply(Article obj, List<Node> value) throws XPathExpressionException, XmlContentException {
          List<String> collabAuthors = Lists.newArrayListWithCapacity(value.size());
          for (Node collabNode : value) {
            collabAuthors.add(collabNode.getTextContent());
          }
          obj.setCollaborativeAuthors(collabAuthors);
        }
      },

      new XmlToObjectOperation.NodeListExpression<Article>(
          "/article/front/article-meta/contrib-group/contrib[@contrib-type=\"author\"]/name") {
        @Override
        protected void apply(Article article, List<Node> authorNames) throws XPathExpressionException, XmlContentException {
          XPath xPath = getXPath();
          List<ArticleAuthor> authors = Lists.newArrayListWithCapacity(authorNames.size());
          for (Node authorName : authorNames) {
            ArticleAuthor author = parsePersonName(authorName, xPath).copyTo(new ArticleAuthor());
            authors.add(author);
          }
          article.setAuthors(authors);
        }
      },

      new XmlToObjectOperation.NodeListExpression<Article>(
          "/article/front/article-meta/contrib-group/contrib[@contrib-type=\"editor\"]/name") {
        @Override
        protected void apply(Article article, List<Node> editorNames) throws XPathExpressionException, XmlContentException {
          XPath xPath = getXPath();
          List<ArticleEditor> editors = Lists.newArrayListWithCapacity(editorNames.size());
          for (Node editorName : editorNames) {
            ArticleEditor editor = parsePersonName(editorName, xPath).copyTo(new ArticleEditor());
            editors.add(editor);
          }
          article.setEditors(editors);
        }
      },

      new XmlToObjectOperation.NodeListExpression<Article>(
          "/article/back/ref-list//citation") {
        @Override
        protected void apply(Article obj, List<Node> citationNodes) throws XPathExpressionException, XmlContentException {
          XPath xPath = getXPath();
          List<CitedArticle> citations = Lists.newArrayListWithCapacity(citationNodes.size());
          for (Node citationNode : citationNodes) {
            citations.add(parseCitation(citationNode, xPath));
          }
          obj.setCitedArticles(citations);
        }
      }

      // TODO More

  });

  // Helper methods for the above

  /**
   * Parse the main category and subcategory from a "Discipline" element, as specified by NLM. If there is a slash in
   * the string, the first one separates the main category from the subcategory. Else, the whole string is the main
   * category.
   * <p/>
   * This is equivalent to capturing groups 1 and 3 from the regex {@code "([^/]*)(/(.*))?"}, but more efficiently.
   *
   * @param categoryString
   * @return
   */
  private static Category parseCategory(String categoryString) {
    Category category = new Category();
    int slashIndex = categoryString.indexOf('/');
    if (slashIndex < 0) {
      category.setMainCategory(categoryString);
    } else {
      category.setMainCategory(categoryString.substring(0, slashIndex));
      category.setSubCategory(categoryString.substring(slashIndex + 1));
    }
    return category;
  }

  private static final String WESTERN_NAME_STYLE = "western";
  private static final String EASTERN_NAME_STYLE = "eastern";

  private static PersonName parsePersonName(Node nameNode, XPath xPath)
      throws XPathExpressionException, XmlContentException {
    String nameStyle = xPath.evaluate("@name-style", nameNode);
    String surname = xPath.evaluate("surname", nameNode);
    String givenName = xPath.evaluate("given-names", nameNode);
    String suffix = xPath.evaluate("suffix", nameNode);

    if (StringUtils.isBlank(surname)) {
      throw new XmlContentException("Required surname is omitted");
    }
    if (StringUtils.isBlank(givenName)) {
      throw new XmlContentException("Required given name is omitted");
    }
    if (StringUtils.isBlank(suffix)) {
      suffix = null;
    }

    String fullName;
    if (WESTERN_NAME_STYLE.equals(nameStyle)) {
      fullName = buildFullName(givenName, surname, suffix);
    } else if (EASTERN_NAME_STYLE.equals(nameStyle)) {
      fullName = buildFullName(surname, givenName, suffix);
    } else {
      throw new XmlContentException("Invalid name-style: " + nameStyle);
    }

    return new PersonName(fullName, givenName, surname, suffix);
  }

  private static String buildFullName(String firstName, String lastName, String suffix) {
    boolean hasSuffix = StringUtils.isNotBlank(suffix);
    int expectedLength = 2 + firstName.length() + lastName.length() + (hasSuffix ? suffix.length() : -1);
    StringBuilder name = new StringBuilder(expectedLength);
    name.append(firstName).append(' ').append(lastName);
    if (hasSuffix) {
      name.append(' ').append(suffix);
    }
    return name.toString();
  }

  private static CitedArticle parseCitation(Node citationNode, XPath xPath) throws XPathExpressionException, XmlContentException {
    CitedArticle citation = new CitedArticle();
    for (XmlToObjectOperation<CitedArticle, ?> operation : CITED_ARTICLE_FIELDS) {
      operation.evaluate(citation, citationNode);
    }
    return citation;
  }

  @SuppressWarnings("unchecked") // can't parameterize array
  private static final ImmutableCollection<XmlToObjectOperation<CitedArticle, ?>> CITED_ARTICLE_FIELDS
      = ImmutableList.<XmlToObjectOperation<CitedArticle, ?>>copyOf(new XmlToObjectOperation[]{

      new XmlToObjectOperation.StringExpression<CitedArticle>("@citation-type") {
        @Override
        protected void apply(CitedArticle obj, String value) throws XPathExpressionException, XmlContentException {
          obj.setCitationType(value);
        }
      },

      new XmlToObjectOperation.StringExpression<CitedArticle>("article-title") {
        @Override
        protected void apply(CitedArticle obj, String value) throws XPathExpressionException, XmlContentException {
          obj.setTitle(value);
        }
      },

      new XmlToObjectOperation.NodeListExpression<CitedArticle>(
          "person-group[@person-group-type=\"author\"]/name") {
        @Override
        protected void apply(CitedArticle obj, List<Node> authorNodes) throws XPathExpressionException, XmlContentException {
          List<CitedArticleAuthor> authors = Lists.newArrayListWithCapacity(authorNodes.size());
          XPath xPath = getXPath();
          for (Node authorNode : authorNodes) {
            CitedArticleAuthor author = parsePersonName(authorNode, xPath).copyTo(new CitedArticleAuthor());
            authors.add(author);
          }
          obj.setAuthors(authors);
        }
      },

      new XmlToObjectOperation.NodeListExpression<CitedArticle>(
          "person-group[@person-group-type=\"editor\"]/name") {
        @Override
        protected void apply(CitedArticle obj, List<Node> editorNodes) throws XPathExpressionException, XmlContentException {
          List<CitedArticleEditor> editors = Lists.newArrayListWithCapacity(editorNodes.size());
          XPath xPath = getXPath();
          for (Node editorNode : editorNodes) {
            CitedArticleEditor editor = parsePersonName(editorNode, xPath).copyTo(new CitedArticleEditor());
            editors.add(editor);
          }
          obj.setEditors(editors);
        }
      },

      // TODO Finish implementing

  });

}
