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
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAuthor;
import org.ambraproject.models.ArticleEditor;
import org.ambraproject.models.ArticlePerson;
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


  protected static abstract class StringExpression extends XmlToObjectOperation.StringExpression<Article> {
    protected StringExpression(String xPathQuery) {
      super(xPathQuery);
    }
  }

  protected static abstract class NodeExpression extends XmlToObjectOperation.NodeExpression<Article> {
    protected NodeExpression(String xPathQuery) {
      super(xPathQuery);
    }
  }

  protected static abstract class NodeListExpression extends XmlToObjectOperation.NodeListExpression<Article> {
    protected NodeListExpression(String xPathQuery) {
      super(xPathQuery);
    }
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
     * be hard-coded. Possible refactor to extract from the XML (from {@code "/article@xml:lang"} perhaps).
     */
    article.setLanguage("en");
  }

  @SuppressWarnings("unchecked") // can't parameterize array
  private static final ImmutableCollection<XmlToObjectOperation<Article, ?>> FIELDS = ImmutableList.<XmlToObjectOperation<Article, ?>>copyOf(new XmlToObjectOperation[]{

      new StringExpression("/article/front/article-meta/article-id[@pub-id-type=\"doi\"]") {
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

      new StringExpression("/article/front/article-meta/title-group/article-title") {
        @Override
        protected void apply(Article obj, String value) {
          obj.setTitle(value);
        }
      },

      new StringExpression("/article/front/journal-meta/issn[@pub-type=\"epub\"]") {
        @Override
        protected void apply(Article obj, String value) {
          obj.seteIssn(value);
        }
      },

      new StringExpression("/article/front/article-meta/abstract") {
        @Override
        protected void apply(Article article, String value) {
          article.setDescription(value);
        }
      },

      new StringExpression("/article/front/article-meta/copyright-statement") {
        @Override
        protected void apply(Article article, String value) {
          article.setRights(value);
        }
      },

      new NodeExpression("/article/front/article-meta/pub-date[@pub-type=\"epub\"]") {
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

      new StringExpression("/article/front/article-meta/elocation-id") {
        @Override
        protected void apply(Article obj, String value) {
          obj.seteLocationId(value);
        }
      },

      new StringExpression("/article/front/article-meta/volume") {
        @Override
        protected void apply(Article obj, String value) {
          obj.setVolume(value);
        }
      },

      new StringExpression("/article/front/article-meta/issue") {
        @Override
        protected void apply(Article obj, String value) {
          obj.setIssue(value);
        }
      },

      new StringExpression("/article/front/journal-meta/journal-title") {
        @Override
        protected void apply(Article obj, String value) {
          obj.setJournal(value);
        }
      },

      new StringExpression("/article/front/journal-meta/publisher/publisher-name") {
        @Override
        protected void apply(Article obj, String value) {
          obj.setPublisherName(value);
        }
      },

      new StringExpression("/article/front/journal-meta/publisher/publisher-loc") {
        @Override
        protected void apply(Article obj, String value) {
          obj.setPublisherLocation(value);
        }
      },

      new NodeListExpression("/article/@article-type") {
        @Override
        protected void apply(Article obj, List<Node> value) {
          Set<String> articleTypes = Sets.newHashSetWithExpectedSize(value.size());
          for (Node node : value) {
            articleTypes.add(node.getTextContent());
          }
          obj.setTypes(articleTypes);
        }
      },

      new NodeListExpression("/article/front/article-meta/contrib-group/contrib[@contrib-type=\"author\"]/name") {
        @Override
        protected void apply(Article article, List<Node> authorNames) throws XPathExpressionException, XmlContentException {
          XPath xPath = getXPath();
          List<ArticleAuthor> authors = Lists.newArrayListWithCapacity(authorNames.size());
          for (Node authorName : authorNames) {
            ArticleAuthor author = parseArticlePerson(new ArticleAuthor(), authorName, xPath);
            authors.add(author);
          }
          article.setAuthors(authors);
        }
      },

      new NodeListExpression("/article/front/article-meta/contrib-group/contrib[@contrib-type=\"editor\"]/name") {
        @Override
        protected void apply(Article article, List<Node> editorNames) throws XPathExpressionException, XmlContentException {
          XPath xPath = getXPath();
          List<ArticleEditor> editors = Lists.newArrayListWithCapacity(editorNames.size());
          for (Node editorName : editorNames) {
            ArticleEditor editor = parseArticlePerson(new ArticleEditor(), editorName, xPath);
            editors.add(editor);
          }
          article.setEditors(editors);
        }
      },

      // TODO More

  });

  // Helper methods for the above

  private static final String WESTERN_NAME_STYLE = "western";
  private static final String EASTERN_NAME_STYLE = "eastern";

  private static <P extends ArticlePerson> P parseArticlePerson(P person, Node nameNode, XPath xPath) throws XPathExpressionException, XmlContentException {
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

    String fullName;
    if (WESTERN_NAME_STYLE.equals(nameStyle)) {
      fullName = buildFullName(givenName, surname, suffix);
    } else if (EASTERN_NAME_STYLE.equals(nameStyle)) {
      fullName = buildFullName(surname, givenName, suffix);
    } else {
      throw new XmlContentException("Invalid name-style: " + nameStyle);
    }

    person.setFullName(fullName);
    person.setSurnames(surname);
    person.setGivenNames(givenName);
    if (StringUtils.isNotBlank(suffix)) {
      person.setSuffix(suffix);
    }
    return person;
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

}
