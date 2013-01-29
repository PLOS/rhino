package org.ambraproject.rhino.service;

import com.google.common.base.Preconditions;
import org.springframework.http.HttpStatus;

/**
 * The result of writing (creating or updating) an entity to the database.
 *
 * @param <T> the type of entity written
 */
public class WriteResult<T> {

  public static enum Action {
    /**
     * A new entity was created.
     */
    CREATED(HttpStatus.CREATED),

    /**
     * An old entity was changed.
     */
    UPDATED(HttpStatus.OK);

    private final HttpStatus status;

    private Action(HttpStatus status) {
      this.status = status;
    }
  }

  private final T writtenObject;
  private final Action action;

  public WriteResult(T writtenObject, Action action) {
    this.action = Preconditions.checkNotNull(action);
    this.writtenObject = Preconditions.checkNotNull(writtenObject);
  }

  /**
   * Indicate whether the write operation created a new entity or updated an existing object.
   *
   * @return the action type
   */
  public Action getAction() {
    return action;
  }

  /**
   * Return the entity that was created or updated.
   *
   * @return the entity
   */
  public T getWrittenObject() {
    return writtenObject;
  }

  /**
   * A status code that can describe the action to an HTTP client.
   *
   * @return the status
   */
  public HttpStatus getStatus() {
    return action.status;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    WriteResult that = (WriteResult) o;

    if (action != that.action) return false;
    if (!writtenObject.equals(that.writtenObject)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = writtenObject.hashCode();
    result = 31 * result + action.hashCode();
    return result;
  }

}
