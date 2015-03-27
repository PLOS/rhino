/*
 * Copyright (c) 2006-2012 by Public Library of Science
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

import com.google.common.io.Closeables;
import org.ambraproject.queue.MessageSender;
import org.ambraproject.queue.MessageServiceImpl;
import org.ambraproject.rhino.content.xml.XpathReader;
import org.ambraproject.rhino.service.AnnotationCrudService;
import org.ambraproject.rhino.service.ArticleStateService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.DummyMessageSender;
import org.ambraproject.rhino.service.impl.AnnotationCrudServiceImpl;
import org.ambraproject.rhino.service.impl.ArticleStateServiceImpl;
import org.ambraproject.rhino.service.impl.AssetCrudServiceImpl;
import org.ambraproject.rhino.service.taxonomy.DummyTaxonomyClassificationService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyClassificationService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyLookupService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.ambraproject.rhino.service.taxonomy.impl.TaxonomyLookupServiceImpl;
import org.ambraproject.rhino.service.taxonomy.impl.TaxonomyServiceImpl;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.service.article.ArticleService;
import org.ambraproject.service.article.ArticleServiceImpl;
import org.ambraproject.service.syndication.SyndicationService;
import org.ambraproject.service.syndication.impl.SyndicationServiceImpl;
import org.ambraproject.testutils.AmbraTestConfigurationFactory;
import org.ambraproject.testutils.HibernateTestSessionFactory;
import org.apache.commons.dbcp.BasicDataSource;
import org.hibernate.SessionFactory;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.yaml.snakeyaml.Yaml;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Configuration
@Import(RhinoConfiguration.class)

// TODO: get tests to work transactionally
/* @EnableTransactionManagement(proxyTargetClass = true) */
public class TestConfiguration extends BaseConfiguration {

  /**
   * Dummy object for sanity-checking the unit test configuration.
   *
   * @return a dummy object
   */
  @Bean
  public Object sampleBean() {
    return new Object();
  }

  @Bean
  public DataSource dataSource() {
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setUrl("jdbc:hsqldb:mem:testdb");
    dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
    return dataSource;
  }

  @Bean
  public LocalSessionFactoryBean sessionFactory(DataSource dataSource) throws IOException {
    LocalSessionFactoryBean bean = new HibernateTestSessionFactory();
    bean.setDataSource(dataSource);
    bean.setSchemaUpdate(true);
    setAmbraMappings(bean);

    Properties hibernateProperties = new Properties();
    hibernateProperties.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
    bean.setHibernateProperties(hibernateProperties);

    return bean;
  }

  @Bean
  public HibernateTransactionManager transactionManager(SessionFactory sessionFactory) {
    HibernateTransactionManager manager = new HibernateTransactionManager();
    manager.setSessionFactory(sessionFactory);
    return manager;
  }

  @Bean
  public org.apache.commons.configuration.Configuration ambraConfiguration() throws Exception {
    return AmbraTestConfigurationFactory.getConfiguration("ambra-test-config.xml");
  }

  @Bean
  public ContentRepoService contentRepoService() {
    return new StubContentRepoService();
  }


  @Bean
  public TaxonomyClassificationService taxonomyClassificationService() {
    return new DummyTaxonomyClassificationService();
  }

  @Bean
  public ArticleService articleService() throws IOException {
    ArticleServiceImpl service = new ArticleServiceImpl();
    service.setSessionFactory(sessionFactory(dataSource()).getObject());

    // TODO: service.setPermissionsService if it's ever needed.
    return service;
  }

  @Bean
  public AssetCrudService assetService() {
    return new AssetCrudServiceImpl();
  }

  @Bean
  public MessageSender messageSender() {
    return new DummyMessageSender();
  }

  @Bean
  public SyndicationService syndicationService() throws Exception {
    SyndicationServiceImpl service = new SyndicationServiceImpl();
    service.setSessionFactory(sessionFactory(dataSource()).getObject());
    service.setAmbraConfiguration(ambraConfiguration());
    MessageServiceImpl messageService = new MessageServiceImpl();
    messageService.setAmbraConfiguration(ambraConfiguration());
    messageService.setSender(messageSender());
    service.setMessageService(messageService);
    return service;
  }

  @Bean
  public ArticleStateService articleStateService() {
    return new ArticleStateServiceImpl();
  }

  @Bean
  public AnnotationCrudService annotationCrudService() {
    return new AnnotationCrudServiceImpl();
  }

  @Bean
  public TaxonomyService taxonomyService() {
    return new TaxonomyServiceImpl();
  }

  @Bean
  public TaxonomyLookupService taxonomyLookupService() {
    return new TaxonomyLookupService() {
      @Override
      public Transceiver read(String journal, String parent) throws IOException {
        throw new UnsupportedOperationException("Dummy service");
      }
    };
  }

  @Bean
  public XpathReader xpathReader() {
    return new XpathReader();
  }

  @Bean
  public RuntimeConfiguration runtimeConfiguration(Yaml yaml) throws Exception {
    YamlConfiguration runtimeConfiguration;
    InputStream is = null;
    boolean threw = true;
    try {
      is = TestConfiguration.class.getClassLoader().getResourceAsStream("rhino-test.yaml");
      runtimeConfiguration = new YamlConfiguration(yaml.loadAs(is, YamlConfiguration.Input.class));
      threw = false;
    } finally {
      Closeables.close(is, threw);
    }

    return runtimeConfiguration;
  }
}
