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

import javax.xml.xpath.XPathExpressionException;
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

  protected static abstract class NodeListExpression extends XmlToObjectOperation.NodeListExpression<Article> {
    protected NodeListExpression(String xPathQuery) {
      super(xPathQuery);
    }
  }


  public static void evaluate(Article article, Document xml) throws XPathExpressionException {
    for (XmlToObjectOperation<Article, ?> field : FIELDS) {
      field.evaluate(article, xml);
    }
  }

  public static final ImmutableCollection<XmlToObjectOperation<Article, ?>> FIELDS = ImmutableList.<XmlToObjectOperation<Article, ?>>copyOf(new XmlToObjectOperation[]{

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

      new NodeListExpression("//person-group") {
        @Override
        protected void apply(Article article, List<Node> value) {
          // TODO
        }
      },

      // TODO More

  });


}
