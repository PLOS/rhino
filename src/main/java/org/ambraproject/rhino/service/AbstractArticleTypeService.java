package org.ambraproject.rhino.service;

import com.google.common.collect.ImmutableSet;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

public abstract class AbstractArticleTypeService implements ArticleTypeService {

  /*
   * Provide a universal implementation for this method, which should be the same for all subclasses.
   *
   * Implementation note: When this codebase is migrated to Java 8, it would be better to move this to a default
   * implementation in ArticleTypeService, and delete the AbstractArticleTypeService class entirely.
   */
  @Override
  public final ImmutableSet<ArticleType> getMetadataForUriStrings(Collection<String> uriStrings) {
    ImmutableSet.Builder<ArticleType> types = ImmutableSet.builder();
    for (String uriString : uriStrings) {
      ArticleType metadata;
      try {
        URI uri = new URI(uriString);
        metadata = getMetadataForUri(uri);
      } catch (URISyntaxException e) {
        metadata = null;
      }
      if (metadata != null) {
        types.add(metadata);
      }
    }
    return types.build();
  }

}
