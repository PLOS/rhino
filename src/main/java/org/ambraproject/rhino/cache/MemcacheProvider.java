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

import org.ambraproject.rhombat.cache.MemcacheClient;
import org.ambraproject.service.cache.Cache;
import org.springframework.beans.factory.annotation.Required;

import java.io.IOException;

/**
 * Implementation of {@link Cache} that uses memcached as its store.
 */
public class MemcacheProvider implements Cache {

  // TODO: org.ambraproject.service.cache.Cache uses keys of type Object, while memcached
  // only operates with String keys.  So this implementation just calls toString() on the
  // objects passed into it.  In practice, this shouldn't matter, since the code we're
  // using that needs a org.ambraproject.service.cache.Cache always uses Strings as
  // keys.  But in the future it might lead to problems.

  // This class delegates to MemcacheClient, which in turn is a thin wrapper around the
  // spymemcached library.
  private MemcacheClient client;

  /**
   * {@inheritDoc}
   */
  @Override
  public Item get(Object key) {
    return new Item(client.get(key.toString()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, E extends Exception> T get(Object key, int refresh, Lookup<T, E> lookup) throws E {

    // This implementation ignores refresh and lets the MemcacheClient control TTL.
    return get(key, lookup);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, E extends Exception> T get(Object key, Lookup<T, E> lookup) throws E {
    T result = client.get(key.toString());
    if (result == null) {
      result = lookup.lookup();
      client.put(key.toString(), result);
    }
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void put(Object key, Item val) {
    client.put(key.toString(), val.getValue());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void remove(Object key) {
    client.remove(key.toString());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeAll() {
    throw new UnsupportedOperationException();
  }

  public void setMemcacheClient(MemcacheClient client) throws IOException {
    this.client = client;
    client.connect();
  }
}
