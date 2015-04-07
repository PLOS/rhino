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

package org.ambraproject.rhino.identity;

import com.google.common.base.Optional;
import org.ambraproject.models.Article;

public class ArticleIdentity extends DoiBasedIdentity {

  private ArticleIdentity(String identifier, Optional<Integer> versionNumber) {
    super(identifier, versionNumber);
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
    return new ArticleIdentity(identifier, Optional.<Integer>absent());
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
  public AssetFileIdentity forXmlAsset() {
    return AssetFileIdentity.create(getIdentifier(), XML_EXTENSION);
  }

  /**
   * @return the "short form" of the DOI for this article that is often used as an internal identifier.  For example, if
   * the full DOI is "10.1371/journal.pone.0077074", this method will return "pone.0077074".
   */
  public String getArticleCode() {
    String identifier = getIdentifier();
    return identifier.substring(identifier.length() - "pone.1234567".length());
  }
}
