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


import com.google.common.base.Preconditions;

import javax.servlet.http.HttpServletRequest;

/**
 * A controller for doing REST actions in the article address space.
 */
public abstract class ArticleController extends RestController {

  private static final String ARTICLE_NAMESPACE = "/article/";
  protected static final String ARTICLE_TEMPLATE = ARTICLE_NAMESPACE + "**";

  private static final String DOI_SCHEME_VALUE = "info:doi/";

  /**
   * Interpret the DOI of an object in the article address space (an article or asset) from a RESTful request.
   * <p/>
   * It would be preferable if possible to parse these values using {@link org.springframework.web.bind.annotation.PathVariable}
   * annotations. Unfortunately, this version of Spring does not support {@code PathVariable}s that span multiple URI
   * components (i.e., have nested slashes), as DOIs do. So we manually get the substring of the URI that is a DOI.
   *
   * @param request a request to the application
   * @return the DOI of the article indicated by the request
   */
  protected static String parseEntityDoi(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    if (!requestUri.startsWith(ARTICLE_NAMESPACE)) {
      // Should be impossible if controllers are mapped correctly
      throw new IllegalArgumentException();
    }
    String doi = requestUri.substring(ARTICLE_NAMESPACE.length());
    return DOI_SCHEME_VALUE + doi;
  }

  /**
   * Prepend the scheme value to a DOI, so it may be used as a database key.
   * <p/>
   * TODO: Replace these with clearer semantics
   *
   * @param doi a DOI with a scheme
   * @return the DOI in the form used as a database key
   * @throws IllegalArgumentException if the DOI is already prefixed with a scheme
   */
  public static String doiToKey(String doi) {
    Preconditions.checkArgument(!doi.startsWith(DOI_SCHEME_VALUE),
        "Attempted to prepend \"%s\" to a DOI that already has it: %s", DOI_SCHEME_VALUE, doi);
    return DOI_SCHEME_VALUE + doi;
  }

  /**
   * Remove the scheme value from a DOI database value, to get the DOI as it appears within NLM documents.
   * <p/>
   * TODO: Replace these with clearer semantics
   *
   * @param key a DOI in the form used as a database key
   * @return a DOI with no scheme value
   * @throws IllegalArgumentException if the DOI does not have a scheme prefix
   */
  public static String keyToDoi(String key) {
    Preconditions.checkArgument(key.startsWith(DOI_SCHEME_VALUE),
        "Expected to start with \"%s\": %s", DOI_SCHEME_VALUE, key);
    return key.substring(DOI_SCHEME_VALUE.length());
  }

}
