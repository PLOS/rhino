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
import org.ambraproject.models.ArticlePerson;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
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
    for (XmlToObjectOperation<Article, ?> field : FIELDS) {
      field.evaluate(article, xml);
    }
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
            year = Integer.parseInt(xPath.evaluate("//year", value));
            month = Integer.parseInt(xPath.evaluate("//month", value));
            day = Integer.parseInt(xPath.evaluate("//day", value));
          } catch (NumberFormatException e) {
            throw new XmlContentException("Expected numbers for date fields", e);
          }

          Calendar date = GregorianCalendar.getInstance();
          date.set(year, month, day);
          obj.setDate(date.getTime());
        }
      },

      new NodeListExpression("/article/@article-type") {
        @Override
        protected void apply(Article obj, List<Node> value) throws XPathExpressionException, XmlContentException {
          Set<String> articleTypes = Sets.newHashSetWithExpectedSize(value.size());
          for (Node node : value) {
            String textContent = node.getTextContent();
            articleTypes.add(textContent);
          }
          obj.setTypes(articleTypes);
        }
      },

      new NodeListExpression("//contrib-group/contrib[@contrib-type=\"author\"]") {
        @Override
        protected List<Node> extract(Node xml) throws XPathExpressionException {
          return super.extract(xml);
        }

        @Override
        protected void apply(Article article, List<Node> value) throws XPathExpressionException, XmlContentException {
          XPath xPath = getXPath();
          List<ArticleAuthor> authors = Lists.newArrayListWithCapacity(value.size());
          //XPathExpression nameExpr = xPath.compile("//name");
          for (Node authorNode : value) {
            // TODO Debug: Gets the first author's name for every node
            Node nameNode = (Node) getXPath().evaluate("//name", authorNode, XPathConstants.NODE);
            ArticleAuthor author = parseArticlePerson(new ArticleAuthor(), nameNode, xPath);
            log.debug(author.getFullName());
            authors.add(author);
          }
          article.setAuthors(authors);
        }
      },

      new NodeExpression("//person-group[@person-group-type=\"editor\"]") {
        @Override
        protected void apply(Article article, Node value) throws XPathExpressionException, XmlContentException {
          // TODO Analogous to author
        }
      },

      // TODO More

  });

  // Helper methods for the above

  private static final String WESTERN_NAME_STYLE = "western";
  private static final String EASTERN_NAME_STYLE = "eastern";

  private static <P extends ArticlePerson> P parseArticlePerson(P person, Node nameNode, XPath xPath) throws XPathExpressionException, XmlContentException {
    String nameStyle = nameNode.getAttributes().getNamedItem("name-style").getTextContent();
    String surname = xPath.evaluate("//surname", nameNode);
    String givenName = xPath.evaluate("//given-names", nameNode);
    String suffix = xPath.evaluate("//suffix", nameNode);
    if (surname == null) {
      throw new XmlContentException("Required surname is omitted");
    }
    if (givenName == null) {
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

    person.setSurnames(surname);
    person.setGivenNames(givenName);
    person.setSuffix(suffix);
    person.setFullName(fullName);
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
