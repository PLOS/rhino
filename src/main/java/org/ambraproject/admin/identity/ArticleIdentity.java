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

package org.ambraproject.admin.identity;

import org.ambraproject.models.Article;

public class ArticleIdentity extends StandAloneIdentity {

  private ArticleIdentity(String identifier) {
    super(identifier);
  }

  /**
   * Create an identifier for an article. The data described by the identifier is the article's XML file.
   * <p/>
   * The DOI provided must <em>not</em> be prefixed with {@code "info:doi/"} or any other URI scheme value.
   *
   * @param identifier the article's DOI
   * @return an identifier for the article
   * @throws IllegalArgumentException if the DOI is prefixed with a URI scheme value or is null or empty
   */
  public static ArticleIdentity create(String identifier) {
    return new ArticleIdentity(identifier);
  }

  /**
   * Create an identifier for an article entity. The data described by the identifier is the article's XML file.
   *
   * @param article an article
   * @return an identifier for the article
   * @throws IllegalArgumentException if the article's DOI is uninitialized or does not start with the expected scheme
   *                                  value
   */
  public static ArticleIdentity create(Article article) {
    String doi = article.getDoi();
    if (doi == null || !doi.startsWith(DOI_SCHEME_VALUE)) {
      throw new IllegalArgumentException("Article does not have expected DOI format: " + doi);
    }
    String identifier = doi.substring(DOI_SCHEME_VALUE.length());
    return create(identifier);
  }

  /**
   * Return an identifier for the asset containing this article's XML file.
   *
   * @return an identity for the XML asset
   */
  public AssetIdentity forXmlAsset() {
    return new AssetIdentity(getIdentifier(), XML_EXTENSION);
  }

}
