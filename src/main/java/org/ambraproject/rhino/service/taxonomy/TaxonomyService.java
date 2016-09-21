package org.ambraproject.rhino.service.taxonomy;

import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.Category;
import org.ambraproject.rhino.view.article.CategoryFlagOutputView;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Services related to article taxonomy.
 */
public interface TaxonomyService extends TaxonomyClassificationService {

  public abstract void flagArticleCategory(Article article, Category category, Optional<Long> userProfileId) throws IOException;

  public abstract void deflagArticleCategory(Article article, Category category, Optional<Long> userProfileId) throws IOException;

  public abstract List<CategoryFlagOutputView> getFlagsCreatedOn(LocalDate fromDate, LocalDate toDate);
}
