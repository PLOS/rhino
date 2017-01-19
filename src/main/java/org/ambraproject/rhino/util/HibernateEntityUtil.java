/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class HibernateEntityUtil {
  private HibernateEntityUtil() {
    throw new AssertionError("Not instantiable");
  }

  @FunctionalInterface
  public static interface EntityCopy<E> {
    public abstract void copy(E source, E destination);
  }

  /**
   * Replace values in a persistent collection of entities without inserting or deleting any more than necessary. Used
   * to work around errors where Hibernate inserts things in a way that conflicts with uniqueness constraints.
   * <p>
   * The values in the replacement collection will completely replace those in the persistent collection with the same
   * key. Entities will be created or deleted as necessary if an entity with a particular key is in one collection but
   * not the other. Other persistent entities will have new data copied into them without their identities being
   * changed.
   * <p>
   * If {@code persistentEntities} is a {@link List}, its contents will be arranged into the same order as in {@code
   * replacementEntities}. Else, new entities will be inserted at the end of {@code persistentEntities}, in the order of
   * {@code replacementEntities}.
   *
   * @param persistentEntities  a collection which may be a persistent Hibernate collection
   * @param replacementEntities the collection of values to replace the previous ones
   * @param keyExtractor        the function that defines a key value, for purposes of matching entities in the two
   *                            collections
   * @param copyFunction        a function that copies values from one entity into another
   * @param <E>                 the entity type
   * @param <K>                 the key type
   */
  public static <E, K> void replaceEntities(Collection<E> persistentEntities,
                                            Collection<E> replacementEntities,
                                            Function<E, K> keyExtractor,
                                            EntityCopy<E> copyFunction) {
    ImmutableMap<K, E> replacementMap = Maps.uniqueIndex(replacementEntities, keyExtractor::apply);
    Maps.uniqueIndex(persistentEntities, keyExtractor::apply); // validate that the key is unique
    Set<K> replaced = new HashSet<>();

    for (Iterator<E> iterator = persistentEntities.iterator(); iterator.hasNext(); ) {
      E persistentEntity = iterator.next();
      K key = keyExtractor.apply(persistentEntity);
      E replacementEntity = replacementMap.get(key);
      replaced.add(key);

      if (replacementEntity == null) {
        iterator.remove();
      } else {
        copyFunction.copy(replacementEntity, persistentEntity);
      }
    }

    // Insert any that weren't deleted or updated
    for (Map.Entry<K, E> entry : replacementMap.entrySet()) {
      if (!replaced.contains(entry.getKey())) {
        persistentEntities.add(entry.getValue());
      }
    }

    if (persistentEntities instanceof List) {
      Comparator<K> keyOrder = Ordering.explicit(replacementMap.keySet().asList());
      Comparator<E> entityOrder = Comparator.comparing(keyExtractor, keyOrder);
      ((List<E>) persistentEntities).sort(entityOrder);
    }
  }

}
