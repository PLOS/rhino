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

import com.google.common.collect.ImmutableSet;

import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.Month;
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


  private static final MultiBucketContentRepoEndpoint NULL_CONTENT_REPO_ENDPOINT = new MultiBucketContentRepoEndpoint() {
    @Override
    public URI getAddress() {
      return null;
    }

    @Override
    public String getDefaultBucket() {
      return null;
    }

    @Override
    public ImmutableSet<String> getAllBuckets() {
      return null;
    }
  };

  private transient MultiBucketContentRepoEndpoint corpusStorageView;

  @Override
  public MultiBucketContentRepoEndpoint getCorpusStorage() {
    return (corpusStorageView != null) ? corpusStorageView
        : (input.contentRepo == null) ? NULL_CONTENT_REPO_ENDPOINT
        : (input.contentRepo.corpus == null) ? NULL_CONTENT_REPO_ENDPOINT
        : (corpusStorageView = new MultiBucketContentRepoEndpoint() {

      private final ImmutableSet<String> buckets;

      {
        ImmutableSet.Builder<String> buckets = ImmutableSet.builder();
        buckets.add(getDefaultBucket());
        if (input.contentRepo.corpus.secondaryBuckets != null) {
          buckets.addAll(input.contentRepo.corpus.secondaryBuckets);
        }
        this.buckets = buckets.build();
      }

      @Override
      public URI getAddress() {
        return input.contentRepo.corpus.address;
      }

      @Override
      public String getDefaultBucket() {
        return input.contentRepo.corpus.bucket;
      }

      @Override
      public ImmutableSet<String> getAllBuckets() {
        return buckets;
      }
    });
  }

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
      public String getDefaultBucket() {
        return input.contentRepo.editorial.bucket;
      }
    });
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

  @Override
  public LocalDate getCompetingInterestPolicyStart() {
    return (input.competingInterestPolicyStart == null) ? DEFAULT_COMPETING_INTEREST_POLICY_START
        : LocalDate.parse(input.competingInterestPolicyStart);
  }

  /**
   * The date at which the relevant software upgrade was deployed on PLOS's Ambra system, which was the only extant
   * Ambra system at the time. Because no other systems will have older comments, it should never be necessary to
   * override this default except in test environments.
   */
  private static final LocalDate DEFAULT_COMPETING_INTEREST_POLICY_START = LocalDate.of(2009, Month.MARCH, 20);


  private transient QueueConfiguration queueConfiguration;

  @Override
  public QueueConfiguration getQueueConfiguration() {
    return (queueConfiguration != null) ? queueConfiguration : (queueConfiguration = new QueueConfiguration() {
      @Override
      public String getSolrUpdate() {
        return input.queue == null ? null : input.queue.solrUpdate;
      }

      @Override
      public String getSolrDelete() {
        return input.queue == null ? null : input.queue.solrDelete;
      }
    });
  }

  private transient ManuscriptCustomMeta manuscriptCustomMeta;

  @Override
  public ManuscriptCustomMeta getManuscriptCustomMeta() {
    return (manuscriptCustomMeta != null) ? manuscriptCustomMeta : (manuscriptCustomMeta = new ManuscriptCustomMeta() {
      @Override
      public String getRevisionDateMetaTagName() {
        return (input.manuscriptCustomMeta != null && input.manuscriptCustomMeta.revisionDate != null)
            ? input.manuscriptCustomMeta.revisionDate
            : "Revision Date";
      }

      @Override
      public String getPublicationStageMetaTagName() {
        return (input.manuscriptCustomMeta != null && input.manuscriptCustomMeta.publicationStage != null)
            ? input.manuscriptCustomMeta.publicationStage
            : "Publication Stage";
      }
    });
  }

  /**
   * @deprecated Temporary; to be removed when versioned ingestion data model is stable.
   */
  @Deprecated
  @Override
  public boolean isUsingVersionedIngestion() {
    return input.usingVersionedIngestion;
  }


  public static class Input {

    private boolean prettyPrintJson = true; // the default value should be true
    private ContentRepoInput contentRepo;
    private HttpConnectionPoolConfigurationInput httpConnectionPool;
    private TaxonomyConfigurationInput taxonomy;
    private UserApiConfigurationInput userApi;
    private boolean usingVersionedIngestion = true; // default is true
    private String competingInterestPolicyStart;
    private QueueConfigurationInput queue;
    private ManuscriptCustomMetaInput manuscriptCustomMeta;

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

    /**
     * @deprecated For reflective access by SnakeYAML only
     */
    @Deprecated
    public void setUserApi(UserApiConfigurationInput userApi) {
      this.userApi = userApi;
    }

    /**
     * @deprecated For reflective access by SnakeYAML only
     */
    @Deprecated
    public void setCompetingInterestPolicyStart(String competingInterestPolicyStart) {
      this.competingInterestPolicyStart = competingInterestPolicyStart;
    }

    /**
     * @deprecated For reflective access by SnakeYAML only
     */
    @Deprecated
    public void setQueue(QueueConfigurationInput queue) {
      this.queue = queue;
    }

    /**
     * @deprecated For reflective access by SnakeYAML only
     */
    @Deprecated
    public void setManuscriptCustomMeta(ManuscriptCustomMetaInput manuscriptCustomMeta) {
      this.manuscriptCustomMeta = manuscriptCustomMeta;
    }

    /**
     * This one will likely be removed in the future, when versioned ingestion is stable and/or the only data schema in
     * use.
     *
     * @deprecated Temporary; for reflective access by SnakeYAML only.
     */
    @Deprecated
    public void setUsingVersionedIngestion(boolean usingVersionedIngestion) {
      this.usingVersionedIngestion = usingVersionedIngestion;
    }
  }

  public static class ContentRepoInput {
    private ContentRepoEndpointInput editorial; // upstairs
    private MultibucketContentRepoEndpointInput corpus;  // downstairs

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
    public void setCorpus(MultibucketContentRepoEndpointInput corpus) {
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

  public static class MultibucketContentRepoEndpointInput extends ContentRepoEndpointInput {
    private List<String> secondaryBuckets;

    /**
     * @deprecated For reflective access by SnakeYAML only
     */
    @Deprecated
    public void setSecondaryBuckets(List<String> secondaryBuckets) {
      this.secondaryBuckets = secondaryBuckets;
    }
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

  public static class QueueConfigurationInput {
    private String solrUpdate;
    private String solrDelete;

    @Deprecated
    public void setSolrUpdate(String solrUpdate) {
      this.solrUpdate = solrUpdate;
    }

    @Deprecated
    public void setSolrDelete(String solrDelete) {
      this.solrDelete = solrDelete;
    }
  }

  public static class ManuscriptCustomMetaInput {
    private String revisionDate;
    private String publicationStage;

    @Deprecated
    public void setRevisionDate(String revisionDate) {
      this.revisionDate = revisionDate;
    }

    @Deprecated
    public void setPublicationStage(String publicationStage) {
      this.publicationStage = publicationStage;
    }
  }

}
