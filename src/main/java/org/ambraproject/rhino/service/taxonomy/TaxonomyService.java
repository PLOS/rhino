package org.ambraproject.rhino.service.taxonomy;

import java.io.IOException;

/**
 * Services related to article taxonomy.
 */
public interface TaxonomyService extends TaxonomyClassificationService, TaxonomyLookupService {

  void flagArticleCategory(Long articleId, Long categoryId, String authId) throws IOException;

  void deflagArticleCategory(Long articleId, Long categoryId, String authId) throws IOException;

}
