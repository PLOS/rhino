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

import java.io.IOException;
import java.io.Serializable;

/**
 * Implementation of {@link Cache} that uses memcached as its store.
 * <p/>
 * Note that one limitation of this implementation is that values must implement {@link Serializable}. I didn't want to
 * add this constraint to {@link Cache}, since it already has implementations that work with non-Serializable objects.
 * If non-Serializable objects are added to this cache, the operation will fail at runtime.
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
    Serializable serializableResult = client.get(key.toString());
    if (serializableResult == null) {
      T result = lookup.lookup();
      client.put(key.toString(), (Serializable) result);
      return result;
    } else {
      return (T) serializableResult;
    }
  }

  /**
   * Adds a key/value pair to the cache, overwriting it if one already exists.
   *
   * @param key cache key
   * @param val Item wrapping the value
   * @throws IllegalArgumentException if the wrapped value does not implement {@link Serializable}
   */
  @Override
  public void put(Object key, Item val) {
    if (!(val.getValue() instanceof Serializable)) {
      throw new IllegalArgumentException("Values inserted must implement java.io.Serializable");
    }
    client.put(key.toString(), (Serializable) val.getValue());
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
