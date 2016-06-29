package org.ambraproject.rhino.service.taxonomy.impl;

import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.Category;
import org.ambraproject.rhino.service.impl.AmbraService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyClassificationService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.ambraproject.rhino.service.taxonomy.WeightedTerm;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.OptionalLong;

public class TaxonomyServiceImpl extends AmbraService implements TaxonomyService {

  @Autowired
  private TaxonomyClassificationService taxonomyClassificationService;

  @Override
  public List<WeightedTerm> classifyArticle(ArticleTable article, Document articleXml) {
    return taxonomyClassificationService.classifyArticle(article, articleXml);
  }

  @Override
  public List<String> getRawTerms(Document articleXml, ArticleTable article, boolean isTextRequired) throws IOException {
    return taxonomyClassificationService.getRawTerms(articleXml, article, isTextRequired);
  }

  @Override
  public void populateCategories(ArticleTable article, Document xml) {
    taxonomyClassificationService.populateCategories(article, xml);
  }

  @Override
  public Collection<Category> getCategoriesForArticle(ArticleTable article) {
    return taxonomyClassificationService.getCategoriesForArticle(article);
  }

  // These methods are a direct copy of the Ambra code found in the TaxonomyServiceImpl
  // We may want to revisit this since it exposes us to unlimited record insertion events by anonymous users

  @Override
  public void flagArticleCategory(long articleId, long categoryId, OptionalLong userId) throws IOException {
    if (userId.isPresent()) {
      //This query will update on a duplicate
      hibernateTemplate.execute(session -> session.createSQLQuery(
          "insert into articleCategoryFlagged(articleId, categoryId, userProfileID, created, lastModified) " +
              "values(:articleId, :categoryId, :userProfileID, :created, :lastModified) " +
              "on duplicate key update lastModified = :lastModified")
          .setLong("articleId", articleId)
          .setLong("categoryId", categoryId)
          .setLong("userProfileID", userId.getAsLong())
          .setCalendar("created", Calendar.getInstance())
          .setCalendar("lastModified", Calendar.getInstance())
          .executeUpdate());
    } else {
      //Insert userProfileID as a null value
      hibernateTemplate.execute(session -> session.createSQLQuery(
          "insert into articleCategoryFlagged(articleId, categoryId, userProfileID, created, lastModified) values(" +
              ":articleId, :categoryId, null, :created, :lastModified)")
          .setLong("articleId", articleId)
          .setLong("categoryId", categoryId)
          .setCalendar("created", Calendar.getInstance())
          .setCalendar("lastModified", Calendar.getInstance())
          .executeUpdate());
    }
  }

  @Override
  public void deflagArticleCategory(long articleId, long categoryId, OptionalLong userId) throws IOException {
    if (userId.isPresent()) {
      hibernateTemplate.execute(session -> session.createSQLQuery(
          "delete acf.* from articleCategoryFlagged acf " +
              "where acf.articleId = :articleId and acf.categoryId = :categoryId and " +
              "acf.userProfileID = :userProfileID")
          .setLong("articleId", articleId)
          .setLong("categoryId", categoryId)
          .setLong("userProfileID", userId.getAsLong())
          .executeUpdate());
    } else {
      //Remove one record from the database at random
      hibernateTemplate.execute(session -> session.createSQLQuery(
          "delete from articleCategoryFlagged where articleId = :articleId and categoryId = :categoryId " +
              "and userProfileID is null limit 1")
          .setLong("articleId", articleId)
          .setLong("categoryId", categoryId)
          .executeUpdate());
    }
  }

}
