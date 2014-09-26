package org.ambraproject.rhino.service;

import org.ambraproject.models.Article;
import org.ambraproject.rhino.util.NlmArticleTypes;

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

  @Override
  public String getNlmArticleType(Article article) {
    String matchedType = null;
    for (String typeString : article.getTypes()) {
      int slashIndex = typeString.lastIndexOf('/');
      if (slashIndex < 0) throw new ArticleTypeException("Article type URI has no slash");
      String nlmTypeCandidate = typeString.substring(slashIndex + 1);
      if (NlmArticleTypes.TYPES.contains(nlmTypeCandidate)) {
        if (matchedType == null) {
          matchedType = nlmTypeCandidate;
        } else {
          String message = String.format(
              "Multiple article type URIs belonging to the same article (DOI=%s) were NLM article-type attributes: %s, %s",
              article.getDoi(), matchedType, nlmTypeCandidate);
          throw new ArticleTypeException(message);
        }
      }
    }
    return matchedType;
  }

  private static ArticleType getMetadataForUri(URI uri) {
    return convertFromLegacy(org.ambraproject.views.article.ArticleType.getKnownArticleTypeForURI(uri));
  }

  private static ArticleType convertFromLegacy(org.ambraproject.views.article.ArticleType legacyArticleType) {
    if (legacyArticleType == null) return null;
    return new ArticleType(legacyArticleType.getUri(),
        legacyArticleType.getHeading(),
        legacyArticleType.getPluralHeading(),
        legacyArticleType.getCode());
  }

  /**
   * {@inheritDoc}
   * <p/>
   * Returns an article type that is defined in the system's {@code ambra.xml} file, which is assumed to be unique for
   * each article.
   */
  @Override
  public ArticleType getArticleType(Article article) {
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
        String message = "An article type URI had invalid syntax (DOI=" + article.getDoi() + ")";
        throw new ArticleTypeException(message, e);
      }

      ArticleType typeForUri = getMetadataForUri(typeUri);
      if (typeForUri != null) { // it is known
        if (matchedType == null) {
          matchedType = typeForUri; // first hit
        } else {
          String message = String.format("Multiple article type URIs belonging to the same article (DOI=%s) were 'known': <%s>, <%s>",
              article.getDoi(), matchedType.getUri(), typeUri);
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
