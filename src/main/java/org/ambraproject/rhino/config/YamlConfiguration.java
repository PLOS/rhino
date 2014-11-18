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

/**
 * Configuration for the server.  This will slowly replace the configuration values in ambra.xml and contain values that
 * are required to start up the server and its behavior.
 */
public class YamlConfiguration implements RuntimeConfiguration {

  // TODO add a validate function that can check for
  // * required values and throw meaning errors when they are not present
  // * supply meaning default values

  private final UserFields uf;

  public YamlConfiguration(UserFields uf) {
    // if the yaml file doesn't contain anything, UserFields object will be null

    if (uf == null) {
      this.uf = new UserFields();
    } else {
      this.uf = uf;
    }
  }

  public static class UserFields {

    private boolean prettyPrintJson = true; // the default value should be true
    private URI contentRepoAddress = null;
    private ContentRepoBuckets contentRepoBuckets;

    public void setPrettyPrintJson(boolean prettyPrintJson) {
      this.prettyPrintJson = prettyPrintJson;
    }

    public void setContentRepoAddress(URI contentRepoAddress) {
      this.contentRepoAddress = contentRepoAddress;
    }

    public void setContentRepoBuckets(ContentRepoBuckets contentRepoBuckets) {
      this.contentRepoBuckets = contentRepoBuckets;
    }

    public static class ContentRepoBuckets {
      private String editorial; // upstairs
      private String corpus;  // downstairs

      public void setEditorial(String editorial) {
        this.editorial = editorial;
      }

      public void setCorpus(String corpus) {
        this.corpus = corpus;
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean prettyPrintJson() {
    return uf.prettyPrintJson;
  }

  @Override
  public URI getContentRepoAddress() {
    return uf.contentRepoAddress;
  }

  @Override
  public String getCorpusBucketName() {
    return (uf.contentRepoBuckets == null) ? null : uf.contentRepoBuckets.corpus;
  }

  @Override
  public String getEditorialBucketName() {
    return (uf.contentRepoBuckets == null) ? null : uf.contentRepoBuckets.editorial;
  }
}
