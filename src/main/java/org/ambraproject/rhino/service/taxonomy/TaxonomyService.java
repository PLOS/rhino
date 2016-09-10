package org.ambraproject.rhino.service.taxonomy;

import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.Category;

import java.io.IOException;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Services related to article taxonomy.
 */
public interface TaxonomyService extends TaxonomyClassificationService {

  void flagArticleCategory(ArticleTable article, Category category, Optional<Long> userProfileId) throws IOException;

  void deflagArticleCategory(ArticleTable article, Category category, Optional<Long> userProfileId) throws IOException;

}
