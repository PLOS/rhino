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

import static com.google.common.base.Strings.isNullOrEmpty;
import java.net.URI;
import com.google.common.base.Preconditions;

public class RuntimeConfiguration {
  private boolean prettyPrintJson = false;
  private URI contentRepoUrl;
  private String editorialBucket;
  private String corpusBucket;
  private URI taxonomyUrl;
  private String thesaurus;
  private String bugsnagApiKey;
  private String bugsnagReleaseStage;

  public RuntimeConfiguration() {
    Preconditions.checkArgument(!isNullOrEmpty(System.getenv("GOOGLE_APPLICATION_CREDENTIALS")),
                                "Please set GOOGLE_APPLICATION_CREDENTIALS.");
  }

  /**
   * determine if json should be formatted or not
   *
   * @return false when formatting is turned off
   */
  public boolean getPrettyPrintJson() {
    return this.prettyPrintJson;
  }

  public void setPrettyPrintJson(boolean prettyPrintJson) {
    this.prettyPrintJson = prettyPrintJson;
  }

  /**
   * Return the content repository bucket for the corpus of articles. The application will write to
   * this bucket when ingesting articles and read from it when serving article assets.
   *
   * @return the corpus bucket name
   */
  public String getCorpusBucket() {
    return this.corpusBucket;
  }

  public void setCorpusBucket(String corpusBucket) {
    Preconditions.checkNotNull(corpusBucket, "CORPUS_BUCKET is required");
    Preconditions.checkState(!corpusBucket.equals(""), "CORPUS_BUCKET is required");
    this.corpusBucket = corpusBucket;
  }

  /*
   * Return the content repository bucket from which the system should pick up editorial
   * (non-article) content.
   * 
   * @return the homepage bucket name
   */
  public String getEditorialBucket() {
    return this.editorialBucket;
  }

  public void setEditorialBucket(String editorialBucket) {
    this.editorialBucket = editorialBucket;
  }

  public URI getTaxonomyUrl() {
    return this.taxonomyUrl;
  }

  public void setTaxonomyUrl(URI taxonomyUrl) {
    Preconditions.checkNotNull(taxonomyUrl, "TAXONOMY_URL is required and must be a valid URL");
    Preconditions.checkState(taxonomyUrl.isAbsolute(),
        "TAXONOMY_URL is required and must be a valid URL");
    this.taxonomyUrl = taxonomyUrl;
  }

  public String getThesaurus() {
    return this.thesaurus;
  }

  public void setThesaurus(String thesaurus) {
    Preconditions.checkNotNull(thesaurus, "THESAURUS is required");
    Preconditions.checkState(!thesaurus.equals(""), "THESAURUS is required");
    this.thesaurus = thesaurus;
  }

  public String getBugsnagApiKey() {
    return this.bugsnagApiKey;
  }

  public void setBugsnagApiKey(String bugsnagApiKey) {
    this.bugsnagApiKey = bugsnagApiKey;
  }

  public String getBugsnagReleaseStage() {
    return this.bugsnagReleaseStage;
  }

  public void setBugsnagReleaseStage(String bugsnagReleaseStage) {
    this.bugsnagReleaseStage = bugsnagReleaseStage;
  }
}
