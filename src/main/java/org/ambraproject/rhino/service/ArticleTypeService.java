package org.ambraproject.rhino.service;

import com.google.common.collect.ImmutableSet;

import java.net.URI;
import java.util.Collection;

/**
 * Access data about article types.
 */
public interface ArticleTypeService {

  /**
   * Get metadata associated with an article type URI. The metadata is a constant object stored by the system.
   *
   * @param uri a URI identifying an article type
   * @return the metadata for the identified article type, or {@code null} if none is stored in the system
   */
  public abstract ArticleType getMetadataForUri(URI uri);

  /**
   * Convenience method for parsing a collection of strings as URIs and looking up article type metadata for all of
   * them.
   *
   * @param uriStrings article type URIs
   * @return metadata for the identified article types
   */
  public abstract ImmutableSet<ArticleType> getMetadataForUriStrings(Collection<String> uriStrings);

}
