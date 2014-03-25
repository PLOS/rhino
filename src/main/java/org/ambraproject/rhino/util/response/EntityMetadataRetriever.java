package org.ambraproject.rhino.util.response;

import com.google.common.base.Preconditions;
import org.ambraproject.models.AmbraEntity;

import java.io.IOException;
import java.util.Calendar;

/**
 * A retriever that translates a timestamped persistent entity into a view.
 *
 * @param <E> the persistent entity type
 * @param <V> the view type
 */
public abstract class EntityMetadataRetriever<E extends AmbraEntity, V> extends MetadataRetriever<V> {

  private E entity = null;

  private E getEntity() {
    return (entity != null) ? entity : (entity = Preconditions.checkNotNull(fetchEntity()));
  }

  /**
   * Retrieve the entity from the persistence tier.
   *
   * @return the entity
   */
  protected abstract E fetchEntity();

  /**
   * {@inheritDoc}
   * <p/>
   * This class fetches the full persistent entity and uses built-in last-modified date. Subclasses may override this
   * method to return the last-modified date without retrieving the rest of the entity for a performance improvement.
   */
  @Override
  protected Calendar getLastModifiedDate() throws IOException {
    return copyToCalendar(getEntity().getLastModified());
  }

  @Override
  protected final V getMetadata() throws IOException {
    return Preconditions.checkNotNull(getView(getEntity()));
  }

  /**
   * Translate the entity into a view object.
   *
   * @param entity the entity
   * @return a view representing the entity
   */
  protected abstract V getView(E entity);

}
