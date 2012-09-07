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

package org.ambraproject.admin.controller;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An identifier for an entity in the article namespace, which can be either an article or an asset.
 */
public class ArticleSpaceId {

  private static final String DOI_SCHEME_VALUE = "info:doi/";
  private static final Pattern URI_PATTERN = Pattern.compile(
      Pattern.quote(ArticleCrudController.ARTICLE_NAMESPACE) + "(.+)\\.(\\w+)");

  /*
   * Internal invariants:
   *   - Instances of this class are immutable.
   *   - All four fields are always non-null (but the Optional object might have an "absent" state, of course).
   *   - The three string fields have length > 0
   *   - key.equals(DOI_SCHEME_VALUE + doi)
   *   - doi and key do not contain ':'
   *   - extension contains no uppercase letters
   *   - If parent has a present value, then that value's parent is absent. (That is, the parent-child relationship has
   *     a maximum depth of 1 and cycles are prohibited.)
   */

  private final String doi;
  private final String key;
  private final String extension;
  private final Optional<ArticleSpaceId> parent;

  private ArticleSpaceId(String doi, String extension, String parentId) {
    super();
    this.doi = Preconditions.checkNotNull(doi);
    this.key = DOI_SCHEME_VALUE + this.doi;
    this.extension = Preconditions.checkNotNull(extension).toLowerCase();
    this.parent = (parentId == null)
        ? Optional.<ArticleSpaceId>absent()
        : Optional.of(forArticle(parentId));

    validate();
  }

  private void validate() {
    Preconditions.checkArgument(doi.indexOf(':') < 0, "DOI must not have scheme prefix (\"info:doi/\")");
    Preconditions.checkArgument(!doi.isEmpty(), "DOI is an empty string");
    Preconditions.checkArgument(!extension.isEmpty(), "Extension is an empty string");
  }

  /**
   * Create an identifier for an article. The data described by the identifier is the article's XML file. On the
   * returned object, {@link #isAsset} will return {@code false}.
   * <p/>
   * The DOI provided must <em>not</em> be prefixed with {@code "info:doi/"} or any other URI scheme value.
   *
   * @param doi the article's DOI
   * @return a new identifier for the article
   * @throws IllegalArgumentException if the DOI is prefixed with a URI scheme value or is null or empty
   */
  public static ArticleSpaceId forArticle(String doi) {
    return new ArticleSpaceId(doi, "xml", null);
  }

  /**
   * Create an identifier for an article assets. On the returned object, {@link #isAsset} will return {@code true}.
   * <p/>
   * The two DOIs provided must <em>not</em> be prefixed with {@code "info:doi/"} or any other URI scheme value.
   *
   * @param assetDoi      the DOI-like identifier of the new asset
   * @param fileExtension the extension of the file that will be associated with this asset in the file store
   * @param articleDoi    the DOI of the article to which the asset belongs
   * @return a new identifier for the asset
   * @throws IllegalArgumentException if a DOI is prefixed with a URI scheme value, or if a DOI or the extension is null
   *                                  or empty
   */
  public static ArticleSpaceId forAsset(String assetDoi, String fileExtension, String articleDoi) {
    return new ArticleSpaceId(assetDoi, fileExtension, Preconditions.checkNotNull(articleDoi));
  }

  /**
   * Parse the identifier from a RESTful request directed at an object in the article namespace.
   *
   * @param request the HTTP request from a REST action
   * @return an for the article or asset to which the REST action was directed
   * @see ArticleCrudController
   */
  public static ArticleSpaceId parse(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    Matcher matcher = URI_PATTERN.matcher(requestUri);
    if (!matcher.matches()) {
      // Valid controller mappings should prevent this
      throw new IllegalArgumentException();
    }

    String parentId = request.getParameter(ArticleCrudController.ASSET_PARAM);
    return new ArticleSpaceId(matcher.group(1), matcher.group(2), parentId);
  }

  /**
   * Return the DOI or DOI-like identifier for the article or asset that this object identifies. The return value will
   * not be prefixed with {@code "info:doi/"} and, under Ambra's current data model, must not be stored in a DOI column
   * of any database table.
   *
   * @return the DOI or DOI-like identifier
   */
  public String getDoi() {
    return doi;
  }

  /**
   * Return a database key value to represent the article or asset that this object identifies. Under Ambra's current
   * data model, this is the string returned by {@link #getDoi} prefixed with {@code "info:doi/"}.
   *
   * @return the key value
   */
  public String getKey() {
    return key;
  }

  /**
   * Get the file extension for the data associated with the identified entity in the file store. File extensions are
   * treated as case-insensitive, so any letters in the returned value are lowercase.
   * <p/>
   * If this object identifies an article, then the associated data is the NLM-format article XML and this method
   * returns {@code "xml"}. If this object identifies an asset, then the file extension represents the data type of that
   * asset.
   *
   * @return the file extension
   */
  public String getExtension() {
    return extension;
  }

  /**
   * Check whether this object identifies an asset.
   * <p/>
   * If this method returns {@code true}, then this object has the identifier for the asset's parent article, which can
   * be retrieved with {@link #getParent()}.
   *
   * @return {@code true} if this object identifies an asset and {@code false} if it identifies an article
   */
  public boolean isAsset() {
    return parent.isPresent();
  }

  /**
   * Return the identifier for the article to which this asset belongs.
   * <p/>
   * This method is safe to call if and only if {@link #isAsset()} returns {@code true}. The returned object is
   * guaranteed to be an article, not an asset, hence {@code this.getParent().isAsset()} is always {@code false}.
   *
   * @return the identifier for the parent article
   * @throws IllegalStateException if this object identifies an article
   */
  public ArticleSpaceId getParent() {
    Preconditions.checkState(parent.isPresent(), "Does not identify an asset");
    return parent.get();
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleSpaceId that = (ArticleSpaceId) o;

    if (!doi.equals(that.doi)) return false;
    if (!extension.equals(that.extension)) return false;
    if (!parent.equals(that.parent)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + doi.hashCode();
    result = prime * result + extension.hashCode();
    result = prime * result + parent.hashCode();
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('(');
    sb.append("doi=\"").append(doi).append('\"');
    sb.append(", extension=\"").append(extension).append('\"');

    sb.append(", parent=");
    if (parent.isPresent()) {
      sb.append('\"').append(parent.get().getDoi()).append('\"');
    } else {
      sb.append((Object) null);
    }

    sb.append(')');
    return sb.toString();
  }

}
