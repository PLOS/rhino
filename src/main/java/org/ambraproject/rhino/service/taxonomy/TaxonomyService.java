package org.ambraproject.rhino.service.taxonomy;

import java.io.IOException;
import java.util.OptionalLong;

/**
 * Services related to article taxonomy.
 */
public interface TaxonomyService extends TaxonomyClassificationService {

  void flagArticleCategory(long articleId, long categoryId, OptionalLong userId) throws IOException;

  void deflagArticleCategory(long articleId, long categoryId, OptionalLong userId) throws IOException;

}
