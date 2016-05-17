/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2013 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.cache;

import com.google.common.base.Strings;
import org.ambraproject.rhino.util.Cache;
import org.ambraproject.rhino.util.NullCache;
import org.ambraproject.rhombat.cache.MemcacheClient;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

/**
 * Factory class that produces an appropriate instance of {@link Cache} based on the runtime configuration in
 * /etc/ambra/ambra.xml.
 */
public final class CacheFactory {

  @Autowired
  private Configuration configuration;

  private static final CacheFactory factory = new CacheFactory();

  private CacheFactory() {
  }

  public static CacheFactory getInstance() {
    return factory;
  }

  public Cache getCache() throws IOException {
    if (Strings.isNullOrEmpty(configuration.getString("ambra.services.memcached.host"))) {
      return new NullCache();
    } else {
      MemcacheClient client = new MemcacheClient(configuration.getString("ambra.services.memcached.host"),
          configuration.getInt("ambra.services.memcached.port"),
          configuration.getString("ambra.services.memcached.appPrefix"),
          configuration.getInt("ambra.services.memcached.defaultTimeout"));
      MemcacheProvider result = new MemcacheProvider();
      result.setMemcacheClient(client);
      return result;
    }
  }
}
