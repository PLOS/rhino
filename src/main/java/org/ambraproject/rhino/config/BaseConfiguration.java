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

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Code common to both the main and testing configurations.
 */
abstract class BaseConfiguration {

  @Inject
  private ApplicationContext context;

  protected void setAmbraMappings(LocalSessionFactoryBean sessionFactoryBean) throws IOException {
    final String mappingLocation = "classpath:org/ambraproject/models/*.hbm.xml";
    Resource[] mappingLocations = context.getResources(mappingLocation);
    if (mappingLocations.length == 0) {
      throw new IllegalStateException("Config error: No Ambra data models found");
    }

    // For performance reasons, we want the ability to lazily load Article.citedArticles.
    // We do this by substituting in our own Hibernate Article mapping, since the one
    // in ambra-models uses eager loading (and changing it in ambra would likely break
    // things there).
    List<Resource> finalResources = new ArrayList<>(mappingLocations.length);
    for (Resource resource : mappingLocations) {
      if (!"Article.hbm.xml".equals(resource.getFilename())) {
        finalResources.add(resource);
      }
    }
    finalResources.add(context.getResource("classpath:ambra/configuration/Article.hbm.xml"));
    sessionFactoryBean.setMappingLocations(finalResources.toArray(new Resource[finalResources.size()]));
  }

}
