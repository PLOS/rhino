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
import java.net.URL;
import java.util.List;
import java.util.Set;
import com.google.common.collect.ImmutableSet;

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


  private transient ContentRepoEndpoint corpusStorageView;

  @Override
  public ContentRepoEndpoint getCorpusStorage() {
    if (corpusStorageView != null) return corpusStorageView;
    if (input.contentRepo == null || input.contentRepo.corpus == null) {
      throw new RuntimeException("contentRepo.corpus must be configured");
    }
    return corpusStorageView = parseCorpusStorage(input.contentRepo.corpus);
  }

  /**
   * For corpus storage, unlike for editorial storage, enforce non-null values and set up collection of all buckets.
   */
  private static ContentRepoEndpoint parseCorpusStorage(ContentRepoEndpointInput corpus) {
    URI address = corpus.address;
    if (address == null) {
      throw new RuntimeException("contentRepo.corpus.address must be configured");
    }

    String bucketName = corpus.bucket;
    if (bucketName == null) {
      throw new RuntimeException("contentRepo.corpus.bucket must be configured");
    }

    return new ContentRepoEndpoint() {
      @Override
      public URI getAddress() {
        return address;
      }

      @Override
      public String getBucketName() {
        return bucketName;
      }
    };
  }


  private static final ContentRepoEndpoint NULL_CONTENT_REPO_ENDPOINT = new ContentRepoEndpoint() {
    @Override
    public URI getAddress() {
      return null;
    }

    @Override
    public String getBucketName() {
      return null;
    }
  };
  private transient ContentRepoEndpoint editorialStorageView;

  @Override
  public ContentRepoEndpoint getEditorialStorage() {
    return (editorialStorageView != null) ? editorialStorageView
        : (input.contentRepo == null) ? NULL_CONTENT_REPO_ENDPOINT
        : (input.contentRepo.editorial == null) ? NULL_CONTENT_REPO_ENDPOINT
        : (editorialStorageView = new ContentRepoEndpoint() {
      @Override
      public URI getAddress() {
        return input.contentRepo.editorial.address;
      }

      @Override
      public String getBucketName() {
        return input.contentRepo.editorial.bucket;
      }
    });
  }

  private final TaxonomyConfiguration taxonomyConfiguration = new TaxonomyConfiguration() {
    @Override
    public URL getServer() {
      return (input.taxonomy == null) ? null : input.taxonomy.server;
    }

    @Override
    public String getThesaurus() {
      return (input.taxonomy == null) ? null : input.taxonomy.thesaurus;
    }
  };

  @Override
  public TaxonomyConfiguration getTaxonomyConfiguration() {
    return taxonomyConfiguration;
  }

  private static class UserApiConfigurationObject implements UserApiConfiguration {
    // Must have real instance variables so that ConfigurationReadController.readNedConfig can serialize it
    private final URL server;
    private final String authorizationAppName;
    private final String authorizationPassword;

    private UserApiConfigurationObject(Input input) {
      server = (input.userApi == null) ? null : input.userApi.server;
      authorizationAppName = (input.userApi == null) ? null : input.userApi.authorizationAppName;
      authorizationPassword = (input.userApi == null) ? null : input.userApi.authorizationPassword;
    }

    @Override
    public URL getServer() {
      return server;
    }

    @Override
    public String getAuthorizationAppName() {
      return authorizationAppName;
    }

    @Override
    public String getAuthorizationPassword() {
      return authorizationPassword;
    }
  }

  private transient UserApiConfigurationObject userApiConfigurationObject;

  @Override
  public UserApiConfiguration getNedConfiguration() {
    return (userApiConfigurationObject != null) ? userApiConfigurationObject
        : (userApiConfigurationObject = new UserApiConfigurationObject(input));
  }

  public static class Input {

    private boolean prettyPrintJson = true; // the default value should be true
    private ContentRepoInput contentRepo;
    private TaxonomyConfigurationInput taxonomy;
    private UserApiConfigurationInput userApi;

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

    /**
     * @deprecated For reflective access by SnakeYAML only
     */
    @Deprecated
    public void setUserApi(UserApiConfigurationInput userApi) {
      this.userApi = userApi;
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
    private URL server;
    private String thesaurus;

    @Deprecated
    public void setServer(URL server) {
      this.server = server;
    }

    @Deprecated
    public void setThesaurus(String thesaurus) {
      this.thesaurus = thesaurus;
    }
  }

  public static class UserApiConfigurationInput {
    private URL server;
    private String authorizationAppName;
    private String authorizationPassword;

    @Deprecated
    public void setServer(URL server) {
      this.server = server;
    }

    @Deprecated
    public void setAuthorizationAppName(String authorizationAppName) {
      this.authorizationAppName = authorizationAppName;
    }

    @Deprecated
    public void setAuthorizationPassword(String authorizationPassword) {
      this.authorizationPassword = authorizationPassword;
    }
  }
}
