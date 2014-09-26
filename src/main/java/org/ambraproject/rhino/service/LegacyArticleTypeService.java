package org.ambraproject.rhino.service;

import org.ambraproject.models.Article;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * Retrieve article types using the imported org.ambraproject.views.article.ArticleType code.
 * <p/>
 * This implementation receives article type metadata from {@code ambra.xml} and stores it in global, static
 * collections. It ought to be rearchitected, but for now we want to just shove it under the {@link ArticleTypeService}
 * interface.
 */
public class LegacyArticleTypeService implements ArticleTypeService {

  /**
   * {@inheritDoc}
   * <p/>
   * This implementation the legacy table of "known" article types.
   */
  @Override
  public ArticleType getMetadataForUri(URI uri) {
    return convertFromLegacy(org.ambraproject.views.article.ArticleType.getKnownArticleTypeForURI(uri));
  }

  private static ArticleType convertFromLegacy(org.ambraproject.views.article.ArticleType legacyArticleType) {
    if (legacyArticleType == null) return null;
    return new ArticleType(legacyArticleType.getUri(),
        legacyArticleType.getHeading(),
        legacyArticleType.getPluralHeading(),
        legacyArticleType.getCode());
  }

  @Override
  public ArticleType getFor(Article article) {
    Set<String> typeUriStrings = article.getTypes();

    /*
     * We expect at most one of these strings to be a "known" article type per the legacy implementation.
     * Return the metadata for that one.
     */
    ArticleType matchedType = null;
    for (String typeUriString : typeUriStrings) {
      URI typeUri;
      try {
        typeUri = new URI(typeUriString);
      } catch (URISyntaxException e) {
        throw new ArticleTypeException("An article type URI had invalid syntax", e);
      }

      ArticleType typeForUri = getMetadataForUri(typeUri);
      if (typeForUri != null) { // it is known
        if (matchedType == null) {
          matchedType = typeForUri; // first hit
        } else {
          String message = String.format("Multiple article type URIs belonging to the same article were 'known': <%s>, <%s>",
              matchedType.getUri(), typeUri);
          throw new ArticleTypeException(message);
        }
      }
    }
    return matchedType;
  }

  /**
   * Indicates that a precondition about article types in the data layer was violated.
   */
  private static class ArticleTypeException extends RuntimeException {
    private ArticleTypeException(String message) {
      super(message);
    }

    private ArticleTypeException(String message, Throwable cause) {
      super(message, cause);
    }
  }

}
