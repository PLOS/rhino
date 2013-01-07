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

import com.google.common.collect.ImmutableSet;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.service.VolumeCrudService;
import org.ambraproject.rhino.service.impl.ArticleCrudServiceImpl;
import org.ambraproject.rhino.service.impl.AssetCrudServiceImpl;
import org.ambraproject.rhino.service.impl.IssueCrudServiceImpl;
import org.ambraproject.rhino.service.impl.VolumeCrudServiceImpl;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;

/**
 * Bean configuration for the application.
 * <p/>
 * This augments some other bean configurations located at {@code src/main/webapp/WEB-INF/spring/appServlet/servlet-context.xml}.
 */
@Configuration
public class RhinoConfiguration extends BaseConfiguration {

  /**
   * Static stuff outside the Spring framework that needs to be run on startup. Ideally this should be empty.
   */
  private static void doStartupKludges() {
    // Required by Ambra; see org.topazproject.ambra.configuration.ConfigurationStore, org.ambraproject.util.URIGenerator
    System.setProperty("SYSTEM_OBJECT_ID_PREFIX", "info:doi/0.0/");
  }

  static {
    doStartupKludges();
  }

  @Bean
  public LocalSessionFactoryBean sessionFactory(DataSource hibernateDataSource) throws IOException {
    LocalSessionFactoryBean bean = new LocalSessionFactoryBean();
    bean.setDataSource(hibernateDataSource);
    setAmbraMappings(bean);

    Properties hibernateProperties = new Properties();
    hibernateProperties.setProperty("hibernate.dialect", org.hibernate.dialect.MySQLDialect.class.getName());
    hibernateProperties.setProperty("hibernate.show_sql", Boolean.FALSE.toString());
    hibernateProperties.setProperty("hibernate.format_sql", Boolean.FALSE.toString());
    bean.setHibernateProperties(hibernateProperties);

    return bean;
  }

  @Bean
  public HibernateTemplate hibernateTemplate(SessionFactory sessionFactory) {
    return new HibernateTemplate(sessionFactory);
  }

  /**
   * Gson instance for serializing Ambra entities into human-friendly JSON.
   */
  @Bean
  public Gson entityGson() {
    GsonBuilder builder = new GsonBuilder();
    builder.setPrettyPrinting();

    final ImmutableSet<String> namesToExclude = ImmutableSet.copyOf(new String[]{
        "ID", // Internal to the database

        // Kludges below; TODO: Debug as needed to remove all
        "journals", // Lazy initialization glitch on Article; not sure if outside our control or not
    });
    builder.setExclusionStrategies(
        new ExclusionStrategy() {
          @Override
          public boolean shouldSkipField(FieldAttributes f) {
            return namesToExclude.contains(f.getName());
          }

          @Override
          public boolean shouldSkipClass(Class<?> clazz) {
            return false;
          }
        }
    );
    return builder.create();
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

}
