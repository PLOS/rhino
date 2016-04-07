package org.ambraproject.rhino.service.taxonomy;

/**
 * Indicates that the remote taxonomy service returned an unusable value.
 */
public class TaxonomyRemoteServiceInvalidBehaviorException extends RuntimeException {
  public TaxonomyRemoteServiceInvalidBehaviorException(String message) {
    super(message);
  }

  public TaxonomyRemoteServiceInvalidBehaviorException(String message, Throwable cause) {
    super(message, cause);
  }
}
