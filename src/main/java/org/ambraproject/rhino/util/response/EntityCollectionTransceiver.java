package org.ambraproject.rhino.util.response;

import com.google.common.base.Preconditions;
import org.ambraproject.models.AmbraEntity;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

/**
 * A "transceiver" that translates a collection of timestamped persistent entities into a view.
 *
 * @param <E> the persistent entity type
 */
public abstract class EntityCollectionTransceiver<E extends AmbraEntity> extends Transceiver {

  private Collection<? extends E> entities = null;

  private Collection<? extends E> getEntities() {
    return (entities != null) ? entities : (entities = Preconditions.checkNotNull(fetchEntities()));
  }

  /**
   * Read the entities from the persistence tier.
   *
   * @return the entity
   */
  protected abstract Collection<? extends E> fetchEntities();

  /**
   * {@inheritDoc}
   * <p/>
   * This class fetches the full set of persistent entities and uses the maximum built-in last-modified date. Subclasses
   * may override this method to return the last-modified date without reading the rest of the data for a performance
   * improvement.
   */
  @Override
  protected Calendar getLastModifiedDate() throws IOException {
    Date lastOfAll = null;
    for (AmbraEntity entity : getEntities()) {
      Date lastModified = entity.getLastModified();
      if (lastOfAll == null || (lastModified != null && lastModified.after(lastOfAll))) {
        lastOfAll = lastModified;
      }
    }
    return (lastOfAll == null) ? null : copyToCalendar(lastOfAll);
  }

  @Override
  protected final Object getMetadata() throws IOException {
    return Preconditions.checkNotNull(getView(getEntities()));
  }

  /**
   * Translate the collection of entities into a view object.
   *
   * @param entities the entities
   * @return a view representing the collection of entities
   */
  protected abstract Object getView(Collection<? extends E> entities);

}
