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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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


  private transient MultiBucketContentRepoEndpoint corpusStorageView;

  @Override
  public MultiBucketContentRepoEndpoint getCorpusStorage() {
    if (corpusStorageView != null) return corpusStorageView;
    if (input.contentRepo == null || input.contentRepo.corpus == null) {
      throw new RuntimeException("contentRepo.corpus must be configured");
    }
    return corpusStorageView = parseCorpusStorage(input.contentRepo.corpus);
  }

  /**
   * For corpus storage, unlike for editorial storage, enforce non-null values and set up collection of all buckets.
   */
  private static MultiBucketContentRepoEndpoint parseCorpusStorage(MultibucketContentRepoEndpointInput corpus) {
    URI address = corpus.address;
    if (address == null) {
      throw new RuntimeException("contentRepo.corpus.address must be configured");
    }

    String defaultBucket = corpus.bucket;
    if (defaultBucket == null) {
      throw new RuntimeException("contentRepo.corpus.bucket must be configured");
    }

    ImmutableSet<String> allBuckets = ImmutableSet.<String>builder()
        .add(defaultBucket)
        .addAll(MoreObjects.firstNonNull(corpus.secondaryBuckets, ImmutableSet.of()))
        .build();

    ImmutableSet<String> secondaryBuckets =
        ImmutableSet.copyOf(MoreObjects.firstNonNull(corpus.secondaryBuckets, ImmutableSet.of()));

    return new MultiBucketContentRepoEndpoint() {
      @Override
      public URI getAddress() {
        return address;
      }

      @Override
      public String getDefaultBucket() {
        return defaultBucket;
      }

      @Override
      public ImmutableSet<String> getAllBuckets() {
        return allBuckets;
      }

      @Override
      public ImmutableSet<String> getSecondaryBuckets() {
        return secondaryBuckets;
      }
    };
  }


  private static final ContentRepoEndpoint NULL_CONTENT_REPO_ENDPOINT = new ContentRepoEndpoint() {
    @Override
    public URI getAddress() {
      return null;
    }

    @Override
    public String getDefaultBucket() {
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
      private static final String DEFAULT_BROKER_URL = "tcp://localhost:61616";
      private static final int DEFAULT_SYNDICATION_RANGE = 30;

      @Override
      public String getBrokerUrl() {
        return input.queue != null && input.queue.brokerUrl != null ? input.queue.brokerUrl : DEFAULT_BROKER_URL;
      }

      @Override
      public String getSolrUpdate() {
        return input.queue != null ? input.queue.solrUpdate : null;
      }

      @Override
      public String getLiteSolrUpdate() {
        return input.queue != null ? input.queue.liteSolrUpdate : null;
      }

      @Override
      public String getSolrDelete() {
        return input.queue != null ? input.queue.solrDelete : null;
      }

      @Override
      public int getSyndicationRange() {
        return input.queue != null && input.queue.syndicationRange != null ? input.queue.syndicationRange : DEFAULT_SYNDICATION_RANGE;
      }
    });
  }

  @Override
  public String getManuscriptCustomMetaName(ManuscriptCustomMetaAttribute attribute) {
    Objects.requireNonNull(attribute);
    if (input.manuscriptCustomMeta == null) return null;
    switch (attribute) {
      case REVISION_DATE:
        return input.manuscriptCustomMeta.revisionDate;
      case PUBLICATION_STAGE:
        return input.manuscriptCustomMeta.publicationStage;
      default:
        throw new AssertionError();
    }
  }


  public static class Input {

    private boolean prettyPrintJson = true; // the default value should be true
    private ContentRepoInput contentRepo;
    private HttpConnectionPoolConfigurationInput httpConnectionPool;
    private TaxonomyConfigurationInput taxonomy;
    private UserApiConfigurationInput userApi;
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
    private String brokerUrl;
    private String solrUpdate;
    private String liteSolrUpdate;
    private String solrDelete;
    private Integer syndicationRange;

    @Deprecated
    public void setBrokerUrl(String brokerUrl) {
      this.brokerUrl = brokerUrl;
    }

    @Deprecated
    public void setSolrUpdate(String solrUpdate) {
      this.solrUpdate = solrUpdate;
    }

    @Deprecated
    public void setLiteSolrUpdate(String liteSolrUpdate) {
      this.liteSolrUpdate = liteSolrUpdate;
    }

    @Deprecated
    public void setSolrDelete(String solrDelete) {
      this.solrDelete = solrDelete;
    }

    @Deprecated
    public void setSyndicationRange(Integer syndicationRange) {
      this.syndicationRange = syndicationRange;
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
