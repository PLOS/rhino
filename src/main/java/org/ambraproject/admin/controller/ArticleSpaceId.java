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

  private final String id;
  private final String extension;
  private final Optional<ArticleSpaceId> parent;

  private ArticleSpaceId(String id, String extension, String parentId) {
    this.id = Preconditions.checkNotNull(id);
    this.extension = Preconditions.checkNotNull(extension).toLowerCase();

    this.parent = (parentId == null)
        ? Optional.<ArticleSpaceId>absent()
        : Optional.of(forArticle(parentId));
  }

  public static ArticleSpaceId forArticle(String doi) {
    return new ArticleSpaceId(doi, "xml", null);
  }

  public static ArticleSpaceId forAsset(String assetDoi, String fileExtension, String articleDoi) {
    return new ArticleSpaceId(assetDoi, fileExtension, Preconditions.checkNotNull(articleDoi));
  }

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

  public String getId() {
    return id;
  }

  public String getKey() {
    return DOI_SCHEME_VALUE + id;
  }

  public String getExtension() {
    return extension;
  }

  public boolean isAsset() {
    return parent.isPresent();
  }

  public ArticleSpaceId getParent() {
    Preconditions.checkState(parent.isPresent(), "Does not identify an asset");
    return parent.get();
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleSpaceId that = (ArticleSpaceId) o;

    if (!id.equals(that.id)) return false;
    if (!extension.equals(that.extension)) return false;
    if (!parent.equals(that.parent)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = 0;
    result = 31 * result + id.hashCode();
    result = 31 * result + extension.hashCode();
    result = 31 * result + parent.hashCode();
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('(');
    sb.append("id='").append(id).append('\'');
    sb.append(", extension='").append(extension).append('\'');
    sb.append(", parent='").append(parent.isPresent() ? parent.get().getId() : null).append('\'');
    sb.append(')');
    return sb.toString();
  }
}
