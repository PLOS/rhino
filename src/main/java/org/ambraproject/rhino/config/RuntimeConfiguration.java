/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.config;

import com.google.common.collect.ImmutableSet;

import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
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
     * @return the name of the bucket on that server to use when none is specified
     */
    String getDefaultBucket();
  }

  interface MultiBucketContentRepoEndpoint extends ContentRepoEndpoint {
    /**
     * @return the names of all buckets that may be specified to use
     */
    ImmutableSet<String> getAllBuckets();

    ImmutableSet<String> getSecondaryBuckets();
  }

  /**
   * Return the content repository bucket for the corpus of articles. The application will write to this bucket when
   * ingesting articles and read from it when serving article assets. Returns {@code null} if no corpus bucket is
   * configured.
   *
   * @return the corpus bucket name
   */
  MultiBucketContentRepoEndpoint getCorpusStorage();

  /**
   * Return the content repository bucket from which the system should pick up editorial (non-article) content. Returns
   * {@code null} if no editorial bucket is configured.
   *
   * @return the homepage bucket name
   */
  ContentRepoEndpoint getEditorialStorage();

  interface TaxonomyConfiguration {
    URL getServer();

    String getThesaurus();

    Set<String> getCategoryBlacklist();
  }

  TaxonomyConfiguration getTaxonomyConfiguration();

  interface UserApiConfiguration {
    URL getServer();

    String getAuthorizationAppName();

    String getAuthorizationPassword();
  }

  UserApiConfiguration getNedConfiguration();

  interface KafkaConfiguration {
    Set<String> getServers();
  }

  KafkaConfiguration getKafkaConfiguration();
}
