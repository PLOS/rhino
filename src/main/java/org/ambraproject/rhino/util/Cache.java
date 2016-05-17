/* $HeadURL::                                                                            $
 * $Id$
 *
 * Copyright (c) 2006-2010 by Public Library of Science
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
package org.ambraproject.rhino.util;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * A simple cache interface
 *
 * @author Pradeep Krishnan
 * @author Joe Osowski
 */
public interface Cache {
  /**
   * Gets an object from the cache.
   *
   * @param key the key
   *
   * @return the value or null if not found in cache
   */
  public Item get(Object key);


  /**
   * Read-thru cache that looks up from cache first and falls back to the supplied look-up.
   * The entry returned by the lookup is committed immediately to the cache and becomes available
   * to all other caches that share the same underlying cache/store. (READ_COMMITTED)
   *
   * @param <T> type of the value expected
   * @param <E> type of the exception expected to be thrown by lookup
   * @param key the key to use for the lookup
   * @param refresh the max-age of entries in the cache (in seconds), or -1 for indefinite
   * @param lookup the lookup to call to get the value if not found in the cache; may be null
   *
   * @return the value in the cache, or returned by the <var>lookup</var> if not in the cache
   *
   * @throws E the exception thrown by lookup
   */
  public <T, E extends Exception> T get(Object key, int refresh, Lookup<T, E> lookup)
                                 throws E;

  /**
   * Read-thru cache that looks up from cache first and falls back to the supplied look-up.
   * The entry returned by the lookup is committed immediately to the cache and becomes available
   * to all other caches that share the same underlying cache/store. (READ_COMMITTED)
   *
   * @param <T> type of the value expected
   * @param <E> type of the exception expected to be thrown by lookup
   * @param key the key to use for the lookup
   * @param lookup the lookup to call to get the value if not found in the cache; may be null
   *
   * @return the value in the cache, or returned by the <var>lookup</var> if not in the cache
   *
   * @throws E the exception thrown by lookup
   */
  public <T, E extends Exception> T get(Object key, Lookup<T, E> lookup)
                                 throws E;

  /**
   * Puts an object into the cache.
   *
   * @param key the key
   * @param val the value
   */
  public void put(Object key, Item val);

  /**
   * Removes an object at the specified key.
   *
   * @param key the key
   */
  public void remove(Object key);

  /**
   * Removes all items from the cache.
   */
  public void removeAll();

  public static interface CachedItem extends Serializable {}

  /**
   * A holder for values that are to be cached. Mainly so that 'null'
   * can be stored for a cache key.
   */
  public static class Item implements CachedItem {
    /** A version compatible with older (ambra 0.9.2 or before) */
    private static final long serialVersionUID = -3696178832412763814L;
    private final Object value;
    private final transient int ttl; // in seconds

    /**
     * Cached item with an infinite time to live.
     *
     * @param value the value to store (could be null)
     */
    public Item(Object value) {
      this(value, -1);
    }

    /**
     * Cached item.
     *
     * @param value the value to store (could be null)
     * @param ttl the time to live in seconds
     */
    public Item(Object value, int ttl) {
      this.value = value;
      this.ttl = ttl;
    }

    public Object getValue() {
      return value;
    }

    public int getTtl() {
      return ttl;
    }

    public String toString() {
      return (value == null) ? "NULL" : value.toString();
    }
  }

  /**
   * The call back to support read-thru cache lookup.
   *
   * @param <T> the type of value looked up
   * @param <E> the type of exception to expect on lookup
   */
  public static abstract class Lookup<T, E extends Exception> {
    /**
     * Looks up a value to populate the cache.
     *
     * @return the value to return; this value (including null) will be put into the cache
     *
     * @throws E from look up.
     */
    public abstract T lookup() throws E;

    /**
     * Executes a read-thru cache operation. The control is given here primarily to support
     * any locking strategies that the lookup implementation provides. By default it executes the
     * supplied operation.
     *
     * @param operation the operation to execute
     *
     * @return the value returned by executing the operation
     *
     * @throws Exception the exception thrown by lookup
     */
    public Item execute(Operation operation) throws Exception {
      return operation.execute(false);
    }

    /**
     * A call-back operation from the Cache during a read-thru cache read.
     */
    public interface Operation {
      public Item execute(boolean degradedMode) throws Exception;
    }
  }

  /**
   * A lookup implementation for Read-thru caches that synchronizes all operations via a
   * monitor lock held on the supplied lock object. This ensures that the cache is only populated
   * by one thread. This mechanism is prone to dead-locks and therefore the LockedLockup should be
   * considered instead.
   *
   * @param <T> the type of value looked up
   * @param <E> the type of exception to expect on lookup
   */
  public static abstract class SynchronizedLookup<T, E extends Exception> extends Lookup<T, E> {
    private Object lock;

    public SynchronizedLookup(Object lock) {
      this.lock = lock;
    }

    public Item execute(Operation operation) throws Exception {
      synchronized (lock) {
        return operation.execute(false);
      }
    }
  }

  /**
   * A lookup implementation for Read-thru caches that synchronizes all operations via a lock
   * held on the supplied lock object. This ensures that the cache is only populated by one
   * thread. This is the preferred lookup strategy since dead-locks are detected by timeouts and
   * degrades to loading from the  backing cache/store.
   *
   * @param <T> the type of value looked up
   * @param <E> the type of exception to expect on lookup
   */
  public static abstract class LockedLookup<T, E extends Exception> extends Lookup<T, E> {
    private Lock     lock;
    private long     timeWait;
    private TimeUnit unit;

    public LockedLookup(Lock lock, long timeWait, TimeUnit unit) {
      this.lock       = lock;
      this.timeWait   = timeWait;
      this.unit       = unit;
    }

    public Item execute(Operation operation) throws Exception {
      boolean acquired = lock.tryLock(timeWait, unit);
      try {
        return operation.execute(!acquired);
      } finally {
        if (acquired)
          lock.unlock();
      }
    }
  }
}
