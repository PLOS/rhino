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
import org.ambraproject.models.Article;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

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

      new StringExpression("//abstract") {
        @Override
        protected void apply(Article article, String value) {
          article.setDescription(value);
        }
      },

      new StringExpression("//copyright-statement") {
        @Override
        protected void apply(Article article, String value) {
          article.setRights(value);
        }
      },

      new NodeExpression("//pub-date[@pub-type=\"epub\"]") {
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

      new NodeListExpression("//person-group") {
        @Override
        protected void apply(Article article, List<Node> value) {
          // TODO
        }
      },

      // TODO More

  });


}
