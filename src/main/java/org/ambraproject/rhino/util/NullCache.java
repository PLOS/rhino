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

package org.ambraproject.rhino.util;

/**
 * Implementation of {@link Cache} that performs no actual caching.
 */
public class NullCache implements Cache {

  /**
   * {@inheritDoc}
   */
  @Override
  public Cache.Item get(Object key) {
    return new Cache.Item(null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, E extends Exception> T get(Object key, int refresh, Cache.Lookup<T, E> lookup)
      throws E {
    return lookup.lookup();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T, E extends Exception> T get(Object key, Cache.Lookup<T, E> lookup) throws E {
    return lookup.lookup();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void put(Object key, Cache.Item val) {}

  /**
   * {@inheritDoc}
   */
  @Override
  public void remove(Object key) {}

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeAll() {}
}
