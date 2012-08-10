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

package org.ambraproject.admin;

import org.ambraproject.admin.service.ArticleCrudService;
import org.ambraproject.admin.service.ArticleCrudServiceImpl;
import org.ambraproject.filestore.FileStoreService;
import org.ambraproject.testutils.HibernateTestSessionFactory;
import org.apache.commons.dbcp.BasicDataSource;
import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;

@Configuration
public class TestConfiguration {

  @Inject
  private ApplicationContext context;

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

    Resource[] mappingLocations = context.getResources("classpath:org/ambraproject/models/*.hbm.xml");
    if (mappingLocations.length == 0) {
      throw new IllegalStateException("Config error: No Ambra data models found");
    }
    bean.setMappingLocations(mappingLocations);

    Properties hibernateProperties = new Properties();
    hibernateProperties.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
    bean.setHibernateProperties(hibernateProperties);

    return bean;
  }

  @Bean
  public HibernateTemplate hibernateTemplate(SessionFactory sessionFactory) {
    return new HibernateTemplate(sessionFactory);
  }

  @Bean
  public ArticleCrudService articleCrudService() {
    return new ArticleCrudServiceImpl();
  }

  @Bean
  public FileStoreService fileStoreService() throws IOException {
    return null; // TODO
  }

}
