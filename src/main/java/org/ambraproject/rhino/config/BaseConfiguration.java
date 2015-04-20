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
import java.util.Map;
import java.util.TreeMap;

/**
 * Code common to both the main and testing configurations.
 */
abstract class BaseConfiguration {

  @Inject
  private ApplicationContext context;

  private Map<String, Resource> getHibernateMappings(String locationPattern) throws IOException {
    Resource[] resources = context.getResources(locationPattern);
    if (resources.length == 0) {
      throw new RuntimeException("Config error: No Hibernate mappings found at " + locationPattern);
    }
    Map<String, Resource> mappings = new TreeMap<>();
    for (Resource resource : resources) {
      mappings.put(resource.getFilename(), resource);
    }
    return mappings;
  }

  protected void setAmbraMappings(LocalSessionFactoryBean sessionFactoryBean) throws IOException {
    final String legacyMappingLocation = "classpath:org/ambraproject/models/*.hbm.xml";
    Map<String, Resource> mappings = getHibernateMappings(legacyMappingLocation);

    // Local mappings override legacy mappings that have the same filename
    final String localMappingLocation = "classpath:ambra/configuration/*.hbm.xml";
    mappings.putAll(getHibernateMappings(localMappingLocation));

    sessionFactoryBean.setMappingLocations(mappings.values().toArray(new Resource[0]));
  }

}
