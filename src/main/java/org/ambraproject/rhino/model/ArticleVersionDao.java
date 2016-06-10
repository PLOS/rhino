package org.ambraproject.rhino.model;

import org.hibernate.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;

public class ArticleVersionDao {

  @Autowired
  protected HibernateTemplate hibernateTemplate;

  public Article getArticle(ArticleVersionIdentifier articleIdentifier) {
    Article article = hibernateTemplate.execute(session -> {
      Query query = session.createSQLQuery("" +
          "FROM ArticleTable article " +
          "WHERE article.doi = :doi");
      query.setParameter("doi", articleIdentifier.getDoi());
      return (Article) query.uniqueResult();
    });
    if (article == null) {
      throw new NoSuchArticleIdException(articleIdentifier);
    }
    return article;
  }

  public ArticleVersion getArticleVersion(ArticleVersionIdentifier articleIdentifier) {
    ArticleVersion articleVersion = hibernateTemplate.execute(session -> {
      Query query = session.createSQLQuery("" +
          "FROM ArticleVersion articleVersion " +
          "INNER JOIN ArticleTable as article " +
          "WHERE articleVersion.versionNumber = :versionNumber AND article.doi = :doi");
      query.setParameter("versionNumber", articleIdentifier.getVersion());
      query.setParameter("doi", articleIdentifier.getDoi());
      return (ArticleVersion) query.uniqueResult();
    });
    if (articleVersion == null) {
      throw new NoSuchArticleIdException(articleIdentifier);
    }
    return articleVersion;
  }

  private class NoSuchArticleIdException extends RuntimeException {
    private NoSuchArticleIdException(ArticleVersionIdentifier articleIdentifier) {
      super("No such article: " + articleIdentifier);
    }
  }
}
