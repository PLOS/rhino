package org.ambraproject.rhino.service.taxonomy;

/**
 * Indicates that a remote service is required to classify articles but is not configured on this system.
 */
public class TaxonomyRemoteServiceNotConfiguredException extends RuntimeException {
  public TaxonomyRemoteServiceNotConfiguredException() {
    super();
  }
}
