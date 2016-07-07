/*
 * Copyright (c) 2006-2014 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.config;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ambraproject.rhino.config.json.AdapterRegistry;
import org.ambraproject.rhino.config.json.DoiBasedIdentitySerializer;
import org.ambraproject.rhino.config.json.ExclusionSpecialCase;
import org.ambraproject.rhino.content.xml.XpathReader;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleListCrudService;
import org.ambraproject.rhino.service.ArticleStateService;
import org.ambraproject.rhino.service.ArticleTypeService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.CamelSender;
import org.ambraproject.rhino.service.CommentCrudService;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.ambraproject.rhino.service.IngestibleService;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.service.LegacyArticleTypeService;
import org.ambraproject.rhino.service.LegacyConfiguration;
import org.ambraproject.rhino.service.MessageSender;
import org.ambraproject.rhino.service.PingbackReadService;
import org.ambraproject.rhino.service.SyndicationCrudService;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.service.impl.ArticleCrudServiceImpl;
import org.ambraproject.rhino.service.impl.ArticleListCrudServiceImpl;
import org.ambraproject.rhino.service.impl.ArticleStateServiceImpl;
import org.ambraproject.rhino.service.impl.AssetCrudServiceImpl;
import org.ambraproject.rhino.service.impl.CommentCrudServiceImpl;
import org.ambraproject.rhino.service.impl.ConfigurationReadServiceImpl;
import org.ambraproject.rhino.service.impl.IngestibleServiceImpl;
import org.ambraproject.rhino.service.impl.IssueCrudServiceImpl;
import org.ambraproject.rhino.service.impl.JournalCrudServiceImpl;
import org.ambraproject.rhino.service.impl.PingbackReadServiceImpl;
import org.ambraproject.rhino.service.impl.SyndicationCrudServiceImpl;
import org.ambraproject.rhino.service.impl.VersionedIngestionService;
import org.ambraproject.rhino.service.impl.VolumeCrudServiceImpl;
import org.ambraproject.rhino.service.taxonomy.TaxonomyClassificationService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.ambraproject.rhino.service.taxonomy.impl.TaxonomyClassificationServiceImpl;
import org.ambraproject.rhino.service.taxonomy.impl.TaxonomyServiceImpl;
import org.ambraproject.rhino.util.GitInfo;
import org.ambraproject.rhino.util.JsonAdapterUtil;
import org.ambraproject.rhino.view.JsonOutputView;
import org.ambraproject.rhino.view.article.ArticleOutputViewFactory;
import org.ambraproject.rhino.view.article.versioned.ArticleIngestionViewFactory;
import org.apache.commons.configuration.ConfigurationException;
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
public class RhinoConfiguration extends BaseConfiguration {

  @Bean
  public AnnotationSessionFactoryBean sessionFactory(DataSource hibernateDataSource) throws IOException {
    // May be switched to true in a dev environment to log SQL code generated by Hibernate.
    // Could be replaced with environmental config if needed.
    final boolean hibernateIsInDebugMode = false;

    AnnotationSessionFactoryBean bean = new AnnotationSessionFactoryBean();
    bean.setDataSource(hibernateDataSource);
    setHibernateMappings(bean);

    Properties hibernateProperties = new Properties();
    hibernateProperties.setProperty("hibernate.dialect", org.hibernate.dialect.MySQLDialect.class.getName());
    hibernateProperties.setProperty("hibernate.show_sql", Boolean.toString(hibernateIsInDebugMode));
    hibernateProperties.setProperty("hibernate.format_sql", Boolean.toString(hibernateIsInDebugMode));
    bean.setHibernateProperties(hibernateProperties);

    bean.setPackagesToScan("org.ambraproject.rhino.model");

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
    builder.setExclusionStrategies(ExclusionSpecialCase.values());
    for (Class<? extends JsonOutputView> viewClass : AdapterRegistry.getOutputViewClasses()) {
      builder.registerTypeAdapter(viewClass, JsonOutputView.SERIALIZER);
    }
    for (Map.Entry<Type, Object> entry : AdapterRegistry.getCustomAdapters().entrySet()) {
      builder.registerTypeAdapter(entry.getKey(), entry.getValue());
    }
    DoiBasedIdentitySerializer.INSTANCE.register(builder);

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
  public org.apache.commons.configuration.Configuration ambraConfiguration() {
    // Fetch from Ambra's custom container
    try {
      return LegacyConfiguration.loadDefaultConfiguration();
    } catch (ConfigurationException e) {
      throw new RuntimeException(e);
    }
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
  public IngestibleService ingestibleService() {
    return new IngestibleServiceImpl();
  }

  @Bean
  public ArticleStateService articleStateService() {
    return new ArticleStateServiceImpl();
  }

  @Bean
  ConfigurationReadService configurationReadService() {
    return new ConfigurationReadServiceImpl();
  }

  @Bean
  public PingbackReadService pingbackReadService() {
    return new PingbackReadServiceImpl();
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
  public ArticleTypeService articleTypeService(org.apache.commons.configuration.Configuration ambraConfiguration) {
    return new LegacyArticleTypeService(ambraConfiguration);
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
  public VersionedIngestionService versionedIngestionService() {
    return new VersionedIngestionService();
  }

  @Bean
  public MessageSender messageSender() {
    return new CamelSender();
  }

  @Bean
  public ArticleOutputViewFactory articleOutputViewFactory() {
    return new ArticleOutputViewFactory();
  }

  @Bean
  public ArticleIngestionViewFactory articleIngestionViewFactory() {
    return new ArticleIngestionViewFactory();
  }

  @Bean
  public XpathReader xpathReader() {
    return new XpathReader();
  }

  @Bean
  public Yaml yaml() {
    return new Yaml();
  }

  @Bean
  public RuntimeConfiguration runtimeConfiguration(Yaml yaml) throws Exception {
    final File configPath = new File("/etc/ambra/rhino.yaml");
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
