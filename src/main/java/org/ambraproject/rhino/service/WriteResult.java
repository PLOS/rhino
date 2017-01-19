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
