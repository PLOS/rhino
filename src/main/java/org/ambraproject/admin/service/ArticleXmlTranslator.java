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

package org.ambraproject.admin.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import org.ambraproject.admin.util.NodeListAdapter;
import org.ambraproject.models.Article;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.List;

/**
 * An XPath query meant to be applied in bulk to an article object.
 */
public abstract class ArticleXmlTranslator<T> {

  private static final Logger log = LoggerFactory.getLogger(ArticleXmlTranslator.class);

  private static final XPath XPATH = XPathFactory.newInstance().newXPath();
  private static final String DOI_PREFIX = "info:doi/";


  protected final String xPathQuery;
  protected final XPathExpression xPathExpression;

  protected ArticleXmlTranslator(String xPathQuery) {
    this.xPathQuery = Preconditions.checkNotNull(xPathQuery);
    try {
      this.xPathExpression = XPATH.compile(this.xPathQuery);
    } catch (XPathExpressionException e) {
      throw new RuntimeException("Could not compile a constant query");
    }
  }

  protected abstract void apply(Article article, T value);

  protected abstract T extract(Document xml) throws XPathExpressionException;

  public final void evaluate(Article article, Document xml) throws XPathExpressionException {
    T value = extract(xml);
    apply(article, value);
  }


  private static abstract class StringExpression extends ArticleXmlTranslator<String> {
    protected StringExpression(String xPathQuery) {
      super(xPathQuery);
    }

    @Override
    protected String extract(Document xml) throws XPathExpressionException {
      return xPathExpression.evaluate(xml);
    }
  }

  private static abstract class NodeListExpression extends ArticleXmlTranslator<List<Node>> {
    protected NodeListExpression(String xPathQuery) {
      super(xPathQuery);
    }

    @Override
    protected List<Node> extract(Document xml) throws XPathExpressionException {
      NodeList result = (NodeList) xPathExpression.evaluate(xml, XPathConstants.NODESET);
      return NodeListAdapter.wrap(result);
    }
  }


  public static final ImmutableCollection<ArticleXmlTranslator<?>> FIELDS = ImmutableList.<ArticleXmlTranslator<?>>copyOf(new ArticleXmlTranslator[]{

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
