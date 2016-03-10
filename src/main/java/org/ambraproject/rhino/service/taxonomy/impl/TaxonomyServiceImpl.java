package org.ambraproject.rhino.service.taxonomy.impl;

import org.ambraproject.models.Article;
import org.ambraproject.rhino.service.impl.AmbraService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyClassificationService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.ambraproject.rhino.service.taxonomy.WeightedTerm;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.w3c.dom.Document;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;

public class TaxonomyServiceImpl extends AmbraService implements TaxonomyService {

  @Autowired
  private TaxonomyClassificationService taxonomyClassificationService;

  @Override
  public List<WeightedTerm> classifyArticle(Document articleXml, Article article) {
    return taxonomyClassificationService.classifyArticle(articleXml, article);
  }

  @Override
  public List<String> getRawTerms(Document articleXml, Article article, boolean isTextRequired) throws IOException {
    return taxonomyClassificationService.getRawTerms(articleXml, article, isTextRequired);
  }

  // These methods are a direct copy of the Ambra code found in the TaxonomyServiceImpl
  // We may want to revisit this since it exposes us to unlimited record insertion events by anonymous users

  @Override
  public void flagArticleCategory(final Long articleId, final Long categoryId, final String authId) throws IOException {

    if (authId != null && authId.length() > 0) {
      //This query will update on a duplicate
      hibernateTemplate.execute(new HibernateCallback<Integer>() {
        public Integer doInHibernate(Session session) throws HibernateException, SQLException {
          return session.createSQLQuery(
              "insert into articleCategoryFlagged(articleId, categoryId, userProfileID, created, lastModified) select " +
                  ":articleId, :categoryId, up.userProfileID, :created, :lastModified " +
                  "from userProfile up where up.authId = :authId on duplicate key update lastModified = :lastModified")
              .setString("authId", authId)
              .setLong("articleId", articleId)
              .setLong("categoryId", categoryId)
              .setCalendar("created", Calendar.getInstance())
              .setCalendar("lastModified", Calendar.getInstance())
              .executeUpdate();
        }
      });
    } else {
      //Insert userProfileID as a null value
      hibernateTemplate.execute(new HibernateCallback<Integer>() {
        public Integer doInHibernate(Session session) throws HibernateException, SQLException {
          return session.createSQLQuery(
              "insert into articleCategoryFlagged(articleId, categoryId, userProfileID, created, lastModified) values(" +
                  ":articleId, :categoryId, null, :created, :lastModified)")
              .setLong("articleId", articleId)
              .setLong("categoryId", categoryId)
              .setCalendar("created", Calendar.getInstance())
              .setCalendar("lastModified", Calendar.getInstance())
              .executeUpdate();
        }
      });
    }
  }

  @Override
  public void deflagArticleCategory(final Long articleId, final Long categoryId, final String authId) throws IOException {
    if (authId != null && authId.length() > 0) {
      hibernateTemplate.execute(new HibernateCallback<Integer>() {
        public Integer doInHibernate(Session session) throws HibernateException, SQLException {
          return session.createSQLQuery(
              "delete acf.* from articleCategoryFlagged acf " +
                  "join userProfile up on up.userProfileID = acf.userProfileID " +
                  "where acf.articleId = :articleId and acf.categoryId = :categoryId and " +
                  "up.authId = :authId")
              .setString("authId", authId)
              .setLong("articleId", articleId)
              .setLong("categoryId", categoryId)
              .executeUpdate();
        }
      });
    } else {
      hibernateTemplate.execute(new HibernateCallback<Integer>() {
        public Integer doInHibernate(Session session) throws HibernateException, SQLException {
          //Remove one record from the database at random
          return session.createSQLQuery(
              "delete from articleCategoryFlagged where articleId = :articleId and categoryId = :categoryId " +
                  "and userProfileID is null limit 1")
              .setLong("articleId", articleId)
              .setLong("categoryId", categoryId)
              .executeUpdate();
        }
      });
    }
  }

}
