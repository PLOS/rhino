/*
 * Copyright (c) 2006-2014 by Public Library of Science
 *
 *    http://plos.org
 *    http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.config;

import java.net.URI;
import java.net.URL;
import java.util.Set;

/**
 * Interface that represents configurable values that are only known at server startup time.
 */
public interface RuntimeConfiguration {

  /**
   * determine if json should be formatted or not
   *
   * @return false when formatting is turned off
   */
  boolean prettyPrintJson();

  /**
   * Identifies a content repo bucket on a particular server.
   */
  interface ContentRepoEndpoint {
    /**
     * @return the URI of the server
     */
    URI getAddress();

    /**
     * @return the name of the bucket on that server to use
     */
    String getBucket();
  }

  /**
   * Return the content repository bucket for the corpus of articles. The application will write to this bucket when
   * ingesting articles and read from it when serving article assets. Returns {@code null} if no corpus bucket is
   * configured.
   *
   * @return the corpus bucket name
   */
  ContentRepoEndpoint getCorpusBucket();

  /**
   * Return the content repository bucket from which the system should pick up editorial (non-article) content. Returns
   * {@code null} if no editorial bucket is configured.
   *
   * @return the homepage bucket name
   */
  ContentRepoEndpoint getEditorialBucket();

  /**
   * TEMPORARY while the versioned-article prototype is under development.
   * <p/>
   * TODO: Unify with {@link #getCorpusBucket}
   */
  ContentRepoEndpoint getVersionedCorpusBucket();

  interface HttpConnectionPoolConfiguration {
    /**
     * @see org.apache.http.pool.ConnPoolControl
     */
    Integer getMaxTotal();

    /**
     * @see org.apache.http.pool.ConnPoolControl
     */
    Integer getDefaultMaxPerRoute();
  }

  HttpConnectionPoolConfiguration getHttpConnectionPoolConfiguration();

  interface TaxonomyConfiguration {
    URL getServer();

    String getThesaurus();

    Set<String> getCategoryBlacklist();
  }

  TaxonomyConfiguration getTaxonomyConfiguration();

}
