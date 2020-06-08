/*
 * Copyright (c) 2017-2019 Public Library of Science
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

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.Storage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ambraproject.rhino.config.json.AdapterRegistry;
import org.ambraproject.rhino.content.xml.CustomMetadataExtractor;
import org.ambraproject.rhino.content.xml.XpathReader;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleDatabaseService;
import org.ambraproject.rhino.service.ArticleListCrudService;
import org.ambraproject.rhino.service.ArticleRevisionWriteService;
import org.ambraproject.rhino.service.CommentCrudService;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.service.ObjectStorageService;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.service.impl.ArticleDatabaseServiceImpl;
import org.ambraproject.rhino.service.impl.ArticleListCrudServiceImpl;
import org.ambraproject.rhino.service.impl.ArticleRevisionWriteServiceImpl;
import org.ambraproject.rhino.service.impl.CommentCrudServiceImpl;
import org.ambraproject.rhino.service.impl.ConfigurationReadServiceImpl;
import org.ambraproject.rhino.service.impl.GCSArticleCrudServiceImpl;
import org.ambraproject.rhino.service.impl.GCSObjectStorageServiceImpl;
import org.ambraproject.rhino.service.impl.IngestionService;
import org.ambraproject.rhino.service.impl.IssueCrudServiceImpl;
import org.ambraproject.rhino.service.impl.JournalCrudServiceImpl;
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
import org.ambraproject.rhino.view.article.RelationshipViewFactory;
import org.ambraproject.rhino.view.comment.CommentNodeView;
import org.ambraproject.rhino.view.journal.ArticleListView;
import org.ambraproject.rhino.view.journal.IssueOutputView;
import org.ambraproject.rhino.view.journal.VolumeOutputView;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

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
                                                     RuntimeConfiguration runtimeConfiguration,
                                                     Gson entityGson) throws IOException {
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
    if (runtimeConfiguration.getPrettyPrintJson()) {
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

  @Bean
  public CloseableHttpClient httpClient() {
    PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();

    manager.setMaxTotal(400);
    manager.setDefaultMaxPerRoute(20);
        
    return HttpClientBuilder.create().setConnectionManager(manager).build();
  }

  @Bean
  public ArticleCrudService articleCrudService(RuntimeConfiguration runtimeConfiguration) {
    return new GCSArticleCrudServiceImpl();
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
  public IngestionService ingestionService() {
    return new IngestionService();
  }

  @Bean
  public ArticleRevisionWriteService articleRevisionWriteService() {
    return new ArticleRevisionWriteServiceImpl();
  }

  @Bean
  public ObjectStorageService objectStorageService(RuntimeConfiguration runtimeConfiguration) {
    return new GCSObjectStorageServiceImpl();
  }

  @Bean
  public ArticleDatabaseService articleDatabaseService() {
    return new ArticleDatabaseServiceImpl();
  }

  @Bean
  public Storage gcs(RuntimeConfiguration runtimeConfiguration) {
    return StorageOptions.newBuilder().setProjectId(runtimeConfiguration.getProjectId()).build().getService();
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
  public RelationshipViewFactory relationshipViewFactory() {
    return new RelationshipViewFactory();
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
  public CommentNodeView.Factory commentNodeViewFactory() {
    return new CommentNodeView.Factory();
  }

  @Bean
  public XpathReader xpathReader() {
    return new XpathReader();
  }

  @Bean
  public GitInfo gitInfo() {
    return new GitInfo();
  }

}
