package org.ambraproject.rhino.service;

import java.net.URI;

/**
 * Retrieve article types using the imported org.ambraproject.views.article.ArticleType code.
 * <p/>
 * This implementation receives article type metadata from {@code ambra.xml} and stores it in global, static
 * collections. It ought to be rearchitected, but for now we want to just shove it under the {@link ArticleTypeService}
 * interface.
 */
public class LegacyArticleTypeService extends AbstractArticleTypeService {

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

}
