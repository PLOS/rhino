package org.ambraproject.rhino.util;

import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
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
   * If {@code persistentEntities} has an order, new entities will be inserted at the end in the same order in which
   * they appear in {@code replacementEntities}.
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
    Map<K, E> replacementMap = Maps.uniqueIndex(replacementEntities, keyExtractor::apply);
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
  }

}
