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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * Configuration for the server.  This will slowly replace the configuration values in ambra.xml and contain values that
 * are required to start up the server and its behavior.
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


  private static final ContentRepoEndpoint NULL_CONTENT_REPO_ENDPOINT = new ContentRepoEndpoint() {
    @Override
    public URI getAddress() {
      return null;
    }

    @Override
    public String getBucket() {
      return null;
    }
  };

  private static ContentRepoEndpoint buildContentRepoEndpointView(final ContentRepoEndpointInput input) {
    return (input == null) ? NULL_CONTENT_REPO_ENDPOINT
        : new ContentRepoEndpoint() {
      @Override
      public URI getAddress() {
        return input.address;
      }

      @Override
      public String getBucket() {
        return input.bucket;
      }
    };
  }

  @Override
  public ContentRepoEndpoint getCorpusBucket() {
    return buildContentRepoEndpointView(
        (input.contentRepo == null) ? null
            : (input.contentRepo.corpus == null) ? null
            : input.contentRepo.corpus);
  }

  @Override
  public ContentRepoEndpoint getEditorialBucket() {
    return buildContentRepoEndpointView(
        (input.contentRepo == null) ? null
            : (input.contentRepo.editorial == null) ? null
            : input.contentRepo.editorial);
  }

  // TEMPORARY for versioned-article prototype development. TODO: Remove
  @Override
  public ContentRepoEndpoint getVersionedCorpusBucket() {
    // For now, use the configured server for the corpus with a hard-coded bucket name.
    // This could be expanded into an actual config value if needed during prototype development.
    final URI corpusServer = Preconditions.checkNotNull(getCorpusBucket().getAddress());
    return new ContentRepoEndpoint() {
      @Override
      public URI getAddress() {
        return corpusServer;
      }

      @Override
      public String getBucket() {
        return "versionedArticlePrototype";
      }
    };
  }

  private final HttpConnectionPoolConfiguration httpConnectionPoolConfiguration = new HttpConnectionPoolConfiguration() {
    @Override
    public Integer getMaxTotal() {
      return (input.httpConnectionPool == null) ? null : input.httpConnectionPool.maxTotal;
    }

    @Override
    public Integer getDefaultMaxPerRoute() {
      return (input.httpConnectionPool == null) ? null : input.httpConnectionPool.defaultMaxPerRoute;
    }
  };

  @Override
  public HttpConnectionPoolConfiguration getHttpConnectionPoolConfiguration() {
    return httpConnectionPoolConfiguration;
  }


  private final TaxonomyConfiguration taxonomyConfiguration = new TaxonomyConfiguration() {
    private ImmutableSet<String> categoryBlacklist;

    @Override
    public URL getServer() {
      return (input.taxonomy == null) ? null : input.taxonomy.server;
    }

    @Override
    public String getThesaurus() {
      return (input.taxonomy == null) ? null : input.taxonomy.thesaurus;
    }

    @Override
    public Set<String> getCategoryBlacklist() {
      if (categoryBlacklist != null) return categoryBlacklist;
      if (input.taxonomy.categoryBlacklist == null) return categoryBlacklist = ImmutableSet.of();
      return categoryBlacklist = ImmutableSet.copyOf(input.taxonomy.categoryBlacklist);
    }
  };

  @Override
  public TaxonomyConfiguration getTaxonomyConfiguration() {
    return taxonomyConfiguration;
  }


  public static class Input {

    private boolean prettyPrintJson = true; // the default value should be true
    private ContentRepoInput contentRepo;
    private HttpConnectionPoolConfigurationInput httpConnectionPool;
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
    public void setHttpConnectionPool(HttpConnectionPoolConfigurationInput httpConnectionPool) {
      this.httpConnectionPool = httpConnectionPool;
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
    private URI address;
    private String bucket;

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

    private final ContentRepoEndpoint immutableView = new ContentRepoEndpoint() {
      @Override
      public URI getAddress() {
        return address;
      }

      @Override
      public String getBucket() {
        return bucket;
      }
    };
  }

  public static class HttpConnectionPoolConfigurationInput {
    private Integer maxTotal;
    private Integer defaultMaxPerRoute;

    @Deprecated
    public void setMaxTotal(Integer maxTotal) {
      this.maxTotal = maxTotal;
    }

    @Deprecated
    public void setDefaultMaxPerRoute(Integer defaultMaxPerRoute) {
      this.defaultMaxPerRoute = defaultMaxPerRoute;
    }
  }

  public static class TaxonomyConfigurationInput {
    private URL server;
    private String thesaurus;
    private List<String> categoryBlacklist;

    @Deprecated
    public void setServer(URL server) {
      this.server = server;
    }

    @Deprecated
    public void setThesaurus(String thesaurus) {
      this.thesaurus = thesaurus;
    }

    @Deprecated
    public void setCategoryBlacklist(List<String> categoryBlacklist) {
      this.categoryBlacklist = categoryBlacklist;
    }
  }

}
