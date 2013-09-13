package org.ambraproject.rhino.view.asset.groomed;

/**
 * Indicates that an asset that does not represent a figure was passed to a figure view.
 */
public class NotAFigureException extends RuntimeException {

  NotAFigureException(String message, Throwable cause) {
    super(message, cause);
  }

  NotAFigureException(String message) {
    super(message);
  }

}
