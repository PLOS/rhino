package org.ambraproject.rhino.util.response;

import org.ambraproject.models.AmbraEntity;

/**
 * A retriever that displays metadata for a timestamped persistent entity by passing the entity directly to the JSON
 * serializer, without translating it into a view object.
 *
 * @param <E> the entity type
 */
public abstract class SimpleEntityMetadataRetriever<E extends AmbraEntity> extends EntityMetadataRetriever<E, E> {

  @Override
  protected E getView(E entity) {
    return entity;
  }

}
