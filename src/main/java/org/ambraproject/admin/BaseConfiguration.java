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

import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Configuration code common to both the main and testing configurations.
 */
public abstract class BaseConfiguration {

  // Common configuration

  @Inject
  protected ApplicationContext context;

  @Bean
  public HibernateTemplate hibernateTemplate(SessionFactory sessionFactory) {
    return new HibernateTemplate(sessionFactory);
  }

  // Helper methods

  protected void setAmbraMappings(LocalSessionFactoryBean sessionFactoryBean) throws IOException {
    final String mappingLocation = "classpath:org/ambraproject/models/*.hbm.xml";
    Resource[] mappingLocations = context.getResources(mappingLocation);
    if (mappingLocations.length == 0) {
      throw new IllegalStateException("Config error: No Ambra data models found");
    }
    sessionFactoryBean.setMappingLocations(mappingLocations);
  }

}
