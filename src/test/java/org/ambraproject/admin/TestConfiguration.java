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

import org.ambraproject.filestore.FileStoreService;
import org.ambraproject.filestore.impl.FileSystemImpl;
import org.ambraproject.testutils.HibernateTestSessionFactory;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

@Configuration
@Import(AdminConfiguration.class)
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

  /**
   * Produce the file store service bean.
   * <p/>
   * This bean-getter has a side effect of creating the mock file store directory if it does not already exist. This
   * should be fixed if possible. There is no side effect to calling it more than once.
   *
   * @return the file store service bean
   * @throws IOException
   */
  @Bean
  public FileStoreService fileStoreService() throws IOException {
    final File topDir = new File("target/test-classes/filestore/");
    topDir.mkdirs(); // TODO Obviate this with Maven?

    final String domain = ""; // Blank for the test environment
    FileStoreService service = new FileSystemImpl(topDir, domain);
    return service;
  }

}
