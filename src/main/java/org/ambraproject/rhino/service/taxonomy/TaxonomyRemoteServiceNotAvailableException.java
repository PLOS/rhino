package org.ambraproject.rhino.service.taxonomy;

import java.io.IOException;
import java.util.Objects;

/**
 * Indicates that a connection to the remote taxonomy service failed.
 */
public class TaxonomyRemoteServiceNotAvailableException extends RuntimeException {
  public TaxonomyRemoteServiceNotAvailableException(IOException cause) {
    super(Objects.requireNonNull(cause));
  }
}
