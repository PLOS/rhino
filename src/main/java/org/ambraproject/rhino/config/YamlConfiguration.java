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

import java.net.URI;

/**
 * Configuration for the server.
 */
public class YamlConfiguration implements RuntimeConfiguration {

  // TODO add a validate function that can check for
  // * required values and throw meaning errors when they are not present
  // * supply meaning default values

  private final Input input;

  public YamlConfiguration(Input input) {
    // if the yaml file doesn't contain anything, UserFields object will be null

    if (input == null) {
      this.input = new Input();
    } else {
      this.input = input;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean prettyPrintJson() {
    return input.prettyPrintJson;
  }


  /**
   * For corpus storage, unlike for editorial storage, enforce non-null values and set up collection of all buckets.
   */
  @Override
  public URI getContentRepoServer() {
    if (input.contentRepo.corpus.address == null) {
      throw new RuntimeException("contentRepo.corpus.address must be configured");
    }
    return input.contentRepo.corpus.address;
  }

  @Override
  public String getCorpusBucket() {
    String bucketName = input.contentRepo.corpus.bucket;
    if (bucketName == null) {
      throw new RuntimeException("contentRepo.corpus.bucket must be configured");
    }
    return bucketName;
  }

  @Override
  public String getEditorialBucket() {
    return input.contentRepo.editorial.bucket;
  }

  @Override
  public URI getTaxonomyServer() {
    return (input.taxonomy == null) ? null : input.taxonomy.server;
  }

  @Override
  public String getThesaurus() {
    return (input.taxonomy == null) ? null : input.taxonomy.thesaurus;
  }

  public static class Input {

    private boolean prettyPrintJson = true; // the default value should be true
    private ContentRepoInput contentRepo;
    private TaxonomyConfigurationInput taxonomy;

    /**
     * @deprecated For reflective access by SnakeYAML only
     */
    @Deprecated
    public void setPrettyPrintJson(boolean prettyPrintJson) {
      this.prettyPrintJson = prettyPrintJson;
    }

    /**
     * @deprecated For reflective access by SnakeYAML only
     */
    @Deprecated
    public void setContentRepo(ContentRepoInput contentRepo) {
      this.contentRepo = contentRepo;
    }

    /**
     * @deprecated For reflective access by SnakeYAML only
     */
    @Deprecated
    public void setTaxonomy(TaxonomyConfigurationInput taxonomy) {
      this.taxonomy = taxonomy;
    }
  }

  public static class ContentRepoInput {
    private ContentRepoEndpointInput editorial; // upstairs
    private ContentRepoEndpointInput corpus;  // downstairs

    /**
     * @deprecated For reflective access by SnakeYAML only
     */
    @Deprecated
    public void setEditorial(ContentRepoEndpointInput editorial) {
      this.editorial = editorial;
    }

    /**
     * @deprecated For reflective access by SnakeYAML only
     */
    @Deprecated
    public void setCorpus(ContentRepoEndpointInput corpus) {
      this.corpus = corpus;
    }
  }

  public static class ContentRepoEndpointInput {
    protected URI address;
    protected String bucket;

    /**
     * @deprecated For reflective access by SnakeYAML only
     */
    @Deprecated
    public void setAddress(URI address) {
      this.address = address;
    }

    /**
     * @deprecated For reflective access by SnakeYAML only
     */
    @Deprecated
    public void setBucket(String bucket) {
      this.bucket = bucket;
    }
  }

  public static class TaxonomyConfigurationInput {
    private URI server;
    private String thesaurus;

    @Deprecated
    public void setServer(URI server) {
      this.server = server;
    }

    @Deprecated
    public void setThesaurus(String thesaurus) {
      this.thesaurus = thesaurus;
    }
  }
}
