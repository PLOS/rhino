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
   * @return the name of the bucket to use.
   */
  public String getS3Bucket();

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

  interface UserApiConfiguration {
    URL getServer();

    String getAuthorizationAppName();

    String getAuthorizationPassword();
  }

  UserApiConfiguration getNedConfiguration();

  /**
   * Return the date, for comments persisted on this system, at which commenters were first prompted for competing
   * interests. Comments from before this date will necessarily lack competing interest statements, but the system
   * should not indicate that the commenters affirmatively declared that they had no competing interests.
   *
   * @return the date at which commenters were first prompted for competing interests
   */
  LocalDate getCompetingInterestPolicyStart();

  interface KafkaConfiguration {
    Set<String> getServers();
  }

  KafkaConfiguration getKafkaConfiguration();


  /**
   * Article attributes that may be parsed from {@code &lt;custom-meta&rt;} elements, whose {@code &lt;meta-name&rt;}
   * values are provided as configuration.
   */
  static enum ManuscriptCustomMetaAttribute {
    REVISION_DATE("revisionDate"),
    PUBLICATION_STAGE("publicationStage");

    private final String configKey;

    private ManuscriptCustomMetaAttribute(String configKey) {
      this.configKey = configKey;
    }

    /**
     * @return the configuration key used to match the attribute to its {@code &lt;meta-name&rt;} value
     */
    public String getConfigKey() {
      return configKey;
    }
  }

  /**
   * Get the {@code &lt;meta-name&rt;} value that will be matched to a {@code &lt;meta-value&rt;} element to populate
   * article metadata.
   *
   * @param attribute an attribute in article metadata
   * @return the &lt;meta-name&rt; value to find in a manuscript
   */
  String getManuscriptCustomMetaName(ManuscriptCustomMetaAttribute attribute);

  /**
   * @return the set of enabled dev features, configured in rhino.yaml.
   */
  ImmutableSet<String> getEnabledDevFeatures();

}
