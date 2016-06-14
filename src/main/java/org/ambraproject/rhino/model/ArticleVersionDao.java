package org.ambraproject.rhino.model;

import org.ambraproject.rhino.identity.ArticleVersionIdentifier;
import org.hibernate.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;

public class ArticleVersionDao {

  @Autowired
  protected HibernateTemplate hibernateTemplate;

  public Article getArticle(ArticleVersionIdentifier versionIdentifier) {
    Article article = hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "FROM ArticleTable article " +
          "WHERE article.doi = :doi");
      query.setParameter("doi", versionIdentifier.getArticleIdentifier().getDoi().getName());
      return (Article) query.uniqueResult();
    });
    if (article == null) {
      throw new NoSuchArticleIdException(versionIdentifier);
    }
    return article;
  }

  public ArticleVersion getArticleVersion(ArticleVersionIdentifier versionIdentifier) {
    ArticleVersion articleVersion = hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "FROM ArticleVersion as av " +
          "WHERE av.revisionNumber = :revisionNumber AND av.article.doi = :doi");
      query.setParameter("revisionNumber", versionIdentifier.getRevision());
      query.setParameter("doi", versionIdentifier.getArticleIdentifier().getDoi().getName());
      return (ArticleVersion) query.uniqueResult();
    });
    if (articleVersion == null) {
      throw new NoSuchArticleIdException(versionIdentifier);
    }
    return articleVersion;
  }

  private class NoSuchArticleIdException extends RuntimeException {
    private NoSuchArticleIdException(ArticleVersionIdentifier versionIdentifier) {
      super("No such article: " + versionIdentifier);
    }
  }
}
