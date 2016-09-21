package org.ambraproject.rhino.service.taxonomy;

import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.Category;

import java.io.IOException;
import java.util.Optional;

/**
 * Services related to article taxonomy.
 */
public interface TaxonomyService extends TaxonomyClassificationService {

  void flagArticleCategory(Article article, Category category, Optional<Long> userProfileId) throws IOException;

  void deflagArticleCategory(Article article, Category category, Optional<Long> userProfileId) throws IOException;

}
