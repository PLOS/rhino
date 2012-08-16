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

import org.ambraproject.models.Article;
import org.w3c.dom.Document;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/*
 * Just a doodle of how XPath queries might be applied in bulk to modify an object.
 */
public enum ArticleXmlTranslator {
  TITLE("") {
    @Override
    protected void apply(Article article, String value) {
      // TODO
    }
  };

  private final String xPathQuery;
  private final XPathExpression xPathExpression;

  private ArticleXmlTranslator(String xPathQuery) {
    this.xPathQuery = xPathQuery;
    try {
      this.xPathExpression = XPathFactory.newInstance().newXPath().compile(this.xPathQuery);
    } catch (XPathExpressionException e) {
      throw new RuntimeException("Could not compile a constant query");
    }
  }

  protected abstract void apply(Article article, String value);

  public void evaluate(Article article, Document xml) throws XPathExpressionException {
    String value = xPathExpression.evaluate(xml);
    apply(article, value);
  }

}
