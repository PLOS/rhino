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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ambraproject.rhino.config.json.AdapterRegistry;
import org.ambraproject.rhino.content.xml.CustomMetadataExtractor;
import org.ambraproject.rhino.content.xml.XpathReader;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleListCrudService;
import org.ambraproject.rhino.service.ArticleRevisionWriteService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.CamelSender;
import org.ambraproject.rhino.service.CommentCrudService;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.ambraproject.rhino.service.ContentRepoPersistenceService;
import org.ambraproject.rhino.service.HibernatePersistenceService;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.service.MessageSender;
import org.ambraproject.rhino.service.SolrIndexService;
import org.ambraproject.rhino.service.SyndicationCrudService;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.service.impl.ArticleCrudServiceImpl;
import org.ambraproject.rhino.service.impl.ArticleListCrudServiceImpl;
import org.ambraproject.rhino.service.impl.ArticleRevisionWriteServiceImpl;
import org.ambraproject.rhino.service.impl.AssetCrudServiceImpl;
import org.ambraproject.rhino.service.impl.CommentCrudServiceImpl;
import org.ambraproject.rhino.service.impl.ConfigurationReadServiceImpl;
import org.ambraproject.rhino.service.impl.ContentRepoPersistenceServiceImpl;
import org.ambraproject.rhino.service.impl.HibernatePersistenceServiceImpl;
import org.ambraproject.rhino.service.impl.IngestionService;
import org.ambraproject.rhino.service.impl.IssueCrudServiceImpl;
import org.ambraproject.rhino.service.impl.JournalCrudServiceImpl;
import org.ambraproject.rhino.service.impl.SolrIndexServiceImpl;
import org.ambraproject.rhino.service.impl.SyndicationCrudServiceImpl;
import org.ambraproject.rhino.service.impl.VolumeCrudServiceImpl;
import org.ambraproject.rhino.service.taxonomy.TaxonomyClassificationService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.ambraproject.rhino.service.taxonomy.impl.TaxonomyClassificationServiceImpl;
import org.ambraproject.rhino.service.taxonomy.impl.TaxonomyServiceImpl;
import org.ambraproject.rhino.util.GitInfo;
import org.ambraproject.rhino.util.Java8TimeGsonAdapters;
import org.ambraproject.rhino.util.JsonAdapterUtil;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.article.ArticleIngestionView;
import org.ambraproject.rhino.view.article.ArticleRevisionView;
import org.ambraproject.rhino.view.article.ItemSetView;
import org.ambraproject.rhino.view.article.RelationshipSetView;
import org.ambraproject.rhino.view.comment.CommentNodeView;
import org.ambraproject.rhino.view.journal.ArticleListView;
import org.ambraproject.rhino.view.journal.IssueOutputView;
import org.ambraproject.rhino.view.journal.VolumeOutputView;
import org.apache.activemq.spring.ActiveMQConnectionFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.hibernate.SessionFactory;
import org.plos.crepo.config.HttpClientFunction;
import org.plos.crepo.service.ContentRepoService;
import org.plos.crepo.service.ContentRepoServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.yaml.snakeyaml.Yaml;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Bean configuration for the application.
 * <p/>
 * This augments some other bean configurations located at {@code src/main/webapp/WEB-INF/spring/appServlet/servlet-context.xml}.
 */
@Configuration
@EnableTransactionManagement
public class RhinoConfiguration {

  @Bean
  public AnnotationSessionFactoryBean sessionFactory(DataSource hibernateDataSource,
                                                     RuntimeConfiguration runtimeConfiguration) throws IOException {
    // May be switched to true in a dev environment to log SQL code generated by Hibernate.
    // Could be replaced with environmental config if needed.
    final boolean hibernateIsInDebugMode = false;

    AnnotationSessionFactoryBean bean = new AnnotationSessionFactoryBean();
    bean.setDataSource(hibernateDataSource);

    Properties hibernateProperties = new Properties();
    hibernateProperties.setProperty("hibernate.dialect", org.hibernate.dialect.MySQLDialect.class.getName());
    hibernateProperties.setProperty("hibernate.show_sql", Boolean.toString(hibernateIsInDebugMode));
    hibernateProperties.setProperty("hibernate.format_sql", Boolean.toString(hibernateIsInDebugMode));
    bean.setHibernateProperties(hibernateProperties);

    bean.setPackagesToScan("org.ambraproject.rhino.model");

    bean.setEntityInterceptor(new HibernateLoggingInterceptor(runtimeConfiguration));

    return bean;
  }

  @Bean
  public HibernateTemplate hibernateTemplate(SessionFactory sessionFactory) {
    return new HibernateTemplate(sessionFactory);
  }

  @Bean

  // We need the @Primary annotation here to tell @EnableTransactionManagement that we want it to use
  // this transaction manager, since we actually have two TransactionManagers in the house: this one
  // and a JmsTransactionManager.
  @Primary
  public HibernateTransactionManager transactionManager(SessionFactory sessionFactory) {
    HibernateTransactionManager manager = new HibernateTransactionManager();
    manager.setSessionFactory(sessionFactory);
    return manager;
  }

  /**
   * Gson instance for serializing Ambra entities into human-friendly JSON.
   */
  @Bean
  public Gson entityGson(RuntimeConfiguration runtimeConfiguration) {
    GsonBuilder builder = JsonAdapterUtil.makeGsonBuilder();
    if (runtimeConfiguration.prettyPrintJson()) {
      builder.setPrettyPrinting();
    }

    // Bulk-apply special cases defined in org.ambraproject.rhino.config.json
    for (Class<? extends JsonOutputView> viewClass : AdapterRegistry.getOutputViewClasses()) {
      builder.registerTypeAdapter(viewClass, JsonOutputView.SERIALIZER);
    }
    for (Map.Entry<Type, Object> entry : AdapterRegistry.getCustomAdapters().entrySet()) {
      builder.registerTypeAdapter(entry.getKey(), entry.getValue());
    }
    Java8TimeGsonAdapters.register(builder);

    return builder.create();
  }

  /**
   * Gson instance for serializing and deserializing {@code userMetadata} fields for the CRepo. Unlike {@link
   * #entityGson}, it requires no adapters (at this time) and should never pretty-print (because we always want to print
   * compact JSON for efficient storage).
   */
  @Bean
  public Gson crepoGson() {
    return new Gson();
  }

  @Bean
  public CloseableHttpClient httpClient(RuntimeConfiguration runtimeConfiguration) {
    PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();

    Integer maxTotal = runtimeConfiguration.getHttpConnectionPoolConfiguration().getMaxTotal();
    manager.setMaxTotal(maxTotal == null ? 400 : maxTotal);

    Integer defaultMaxPerRoute = runtimeConfiguration.getHttpConnectionPoolConfiguration().getDefaultMaxPerRoute();
    manager.setDefaultMaxPerRoute(defaultMaxPerRoute == null ? 20 : defaultMaxPerRoute);

    return HttpClientBuilder.create().setConnectionManager(manager).build();
  }

  @Bean
  public ContentRepoService contentRepoService(RuntimeConfiguration runtimeConfiguration,
                                               final CloseableHttpClient httpClient) {
    RuntimeConfiguration.ContentRepoEndpoint corpus = runtimeConfiguration.getCorpusStorage();
    final String repoServer = Preconditions.checkNotNull(corpus.getAddress().toString());
    Objects.requireNonNull(httpClient);

    return new ContentRepoServiceImpl(repoServer, HttpClientFunction.from(httpClient));
  }

  @Bean
  public ActiveMQConnectionFactory jmsConnectionFactory(RuntimeConfiguration runtimeConfiguration) {
    ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
    factory.setBrokerURL(runtimeConfiguration.getQueueConfiguration().getBrokerUrl());
    return factory;
  }


  @Bean
  public ArticleCrudService articleCrudService() {
    return new ArticleCrudServiceImpl();
  }

  @Bean
  public AssetCrudService assetCrudService() {
    return new AssetCrudServiceImpl();
  }

  @Bean
  public VolumeCrudService volumeCrudService() {
    return new VolumeCrudServiceImpl();
  }

  @Bean
  public IssueCrudService issueCrudService() {
    return new IssueCrudServiceImpl();
  }

  @Bean
  public SolrIndexService solrIndexService() {
    return new SolrIndexServiceImpl();
  }

  @Bean
  ConfigurationReadService configurationReadService() {
    return new ConfigurationReadServiceImpl();
  }

  @Bean
  public JournalCrudService journalCrudService() {
    return new JournalCrudServiceImpl();
  }

  @Bean
  public CommentCrudService annotationCrudService() {
    return new CommentCrudServiceImpl();
  }

  @Bean
  public TaxonomyService taxonomyService() {
    return new TaxonomyServiceImpl();
  }

  @Bean
  public TaxonomyClassificationService taxonomyClassificationService() {
    return new TaxonomyClassificationServiceImpl();
  }

  @Bean
  public ArticleListCrudService collectionCrudService() {
    return new ArticleListCrudServiceImpl();
  }

  @Bean
  public SyndicationCrudService syndicationService() {
    return new SyndicationCrudServiceImpl();
  }

  @Bean
  public IngestionService ingestionService() {
    return new IngestionService();
  }

  @Bean
  public ArticleRevisionWriteService articleRevisionWriteService() {
    return new ArticleRevisionWriteServiceImpl();
  }

  @Bean
  public ContentRepoPersistenceService contentRepoPersistenceService() {
    return new ContentRepoPersistenceServiceImpl();
  }

  @Bean
  public HibernatePersistenceService hibernatePersistenceService() {
    return new HibernatePersistenceServiceImpl();
  }

  @Bean
  public MessageSender messageSender() {
    return new CamelSender();
  }

  @Bean
  public CustomMetadataExtractor.Factory customMetadataExtractorFactory() {
    return new CustomMetadataExtractor.Factory();
  }

  @Bean
  public ArticleIngestionView.Factory articleIngestionViewFactory() {
    return new ArticleIngestionView.Factory();
  }

  @Bean
  public RelationshipSetView.Factory relationshipSetViewFactory() {
    return new RelationshipSetView.Factory();
  }

  @Bean
  public IssueOutputView.Factory issueOutputViewFactory() {
    return new IssueOutputView.Factory();
  }

  @Bean
  public VolumeOutputView.Factory volumeOutputViewFactory() {
    return new VolumeOutputView.Factory();
  }

  @Bean
  public ItemSetView.Factory itemSetViewFactory() {
    return new ItemSetView.Factory();
  }

  @Bean
  public ArticleRevisionView.Factory articleRevisionViewFactory() {
    return new ArticleRevisionView.Factory();
  }

  @Bean
  public ArticleListView.Factory articleListViewFactory() {
    return new ArticleListView.Factory();
  }

  @Bean
  public CommentNodeView.Factory commentNodeViewFactory(RuntimeConfiguration runtimeConfiguration) {
    return new CommentNodeView.Factory(runtimeConfiguration);
  }

  @Bean
  public XpathReader xpathReader() {
    return new XpathReader();
  }

  @Bean
  public Yaml yaml() {
    return new Yaml();
  }


  private static final String CONFIG_DIR_PROPERTY_NAME = "rhino.configDir";

  static File getConfigDirectory() {
    String property = System.getProperty(CONFIG_DIR_PROPERTY_NAME);
    if (!Strings.isNullOrEmpty(property)) {
      return new File(property);
    } else {
      throw new RuntimeException("Config directory not found. " + CONFIG_DIR_PROPERTY_NAME + " must be defined.");
    }
  }

  @Bean
  public RuntimeConfiguration runtimeConfiguration(Yaml yaml)
      throws IOException {
    File configDir = getConfigDirectory();
    File configPath = new File(configDir, "rhino.yaml");
    if (!configPath.exists()) {
      throw new RuntimeConfigurationException(configPath.getAbsolutePath() + " not found");
    }

    YamlConfiguration runtimeConfiguration;
    try (Reader reader = new BufferedReader(new FileReader(configPath))) {
      YamlConfiguration.Input configValues = yaml.loadAs(reader, YamlConfiguration.Input.class);
      runtimeConfiguration = new YamlConfiguration(configValues);
    }

    return runtimeConfiguration;
  }

  @Bean
  public GitInfo gitInfo() {
    return new GitInfo();
  }

}
