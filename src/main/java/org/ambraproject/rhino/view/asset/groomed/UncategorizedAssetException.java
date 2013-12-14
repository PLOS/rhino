package org.ambraproject.rhino.view.asset.groomed;

/**
 * Indicates that an asset that could not be categorized into an image type.
 */
public class UncategorizedAssetException extends RuntimeException {

  UncategorizedAssetException(String message, Throwable cause) {
    super(message, cause);
  }

  UncategorizedAssetException(String message) {
    super(message);
  }

}
