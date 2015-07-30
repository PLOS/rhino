package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.ambraproject.models.AmbraEntity;
import org.ambraproject.models.Article;
import org.ambraproject.models.Journal;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.model.ArticleCollection;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.CollectionCrudService;
import org.ambraproject.rhino.util.response.EntityTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CollectionCrudServiceImpl extends AmbraService implements CollectionCrudService {

  @Override
  public ArticleCollection create(String journalKey, String slug, String title, Set<ArticleIdentity> articleIds) {
    ArticleCollection coll = new ArticleCollection();
    coll.setSlug(slug);
    coll.setTitle(title);

    Journal journal = (Journal) DataAccessUtils.uniqueResult(hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Journal.class)
            .add(Restrictions.eq("journalKey", journalKey))));
    if (journal == null) {
      throw new RestClientException("Journal not found: " + journalKey, HttpStatus.BAD_REQUEST);
    }
    coll.setJournal(journal);

    List<Article> articles = fetchArticles(articleIds);
    coll.setArticles(articles);

    hibernateTemplate.persist(coll);
    return coll;
  }

  private ArticleCollection getCollection(final String journalKey, final String slug) {
    return DataAccessUtils.uniqueResult(hibernateTemplate.execute(new HibernateCallback<List<ArticleCollection>>() {
      @Override
      public List<ArticleCollection> doInHibernate(Session session) throws HibernateException, SQLException {
        Query query = session.createQuery("" +
            "from ArticleCollection c " +
            "where c.slug=:slug and c.journal.journalKey=:journalKey");
        query.setString("slug", slug);
        query.setString("journalKey", journalKey);
        return query.list();
      }
    }));
  }

  @Override
  public ArticleCollection update(final String journalKey, final String slug,
                                  String title, Set<ArticleIdentity> articleIds) {
    ArticleCollection coll = getCollection(journalKey, slug);
    if (coll == null) {
      throw new RestClientException("Collection does not exist", HttpStatus.NOT_FOUND);
    }

    if (title != null) {
      coll.setTitle(title);
    }

    if (articleIds != null) {
      Preconditions.checkArgument(!articleIds.isEmpty());
      List<Article> newArticles = fetchArticles(articleIds);
      List<Article> oldArticles = coll.getArticles();
      oldArticles.clear();
      oldArticles.addAll(newArticles);
    }

    hibernateTemplate.update(coll);
    return coll;
  }

  private List<Article> fetchArticles(Set<ArticleIdentity> articleIds) {
    final Map<String, Integer> articleKeys = Maps.newHashMapWithExpectedSize(articleIds.size());
    int i = 0;
    for (ArticleIdentity articleId : articleIds) {
      articleKeys.put(articleId.getKey(), i++);
    }
    List<Article> articles = (List<Article>) hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Article.class)
            .add(Restrictions.in("doi", articleKeys.keySet())));
    if (articles.size() < articleIds.size()) {
      throw new RestClientException(buildMissingArticleMessage(articles, articleKeys.keySet()), HttpStatus.BAD_REQUEST);
    }

    // Since Hibernate will maintain an order, sort articles into the same order in which IDs were submitted.
    // Order doesn't currently matter, but prefer this over the arbitrary order in which the query returned them.
    Collections.sort(articles, new Comparator<Article>() {
      @Override
      public int compare(Article o1, Article o2) {
        return articleKeys.get(o1.getDoi()) - articleKeys.get(o2.getDoi());
      }
    });
    return articles;
  }

  private static String buildMissingArticleMessage(Collection<Article> foundArticles, Set<String> requestedArticleKeys) {
    Set<String> foundArticleKeys = Sets.newHashSetWithExpectedSize(foundArticles.size());
    for (Article article : foundArticles) {
      foundArticleKeys.add(ArticleIdentity.create(article).getKey());
    }
    String[] badDois = Sets.difference(requestedArticleKeys, foundArticleKeys).toArray(new String[0]);
    Arrays.sort(badDois);
    return "Articles not found with DOIs: " + new Gson().toJson(badDois);
  }

  @Override
  public Transceiver read(final String journalKey, final String slug) {
    return new EntityTransceiver() {
      @Override
      protected ArticleCollection fetchEntity() {
        ArticleCollection result = getCollection(journalKey, slug);
        if (result == null) {
          String message = String.format("No collection exists in journal=%s with slug=%s", journalKey, slug);
          throw new RestClientException(message, HttpStatus.NOT_FOUND);
        }
        return result;
      }

      @Override
      protected Object getView(AmbraEntity entity) {
        return entity;
      }
    };
  }

  @Override
  public Collection<ArticleCollection> getContainingCollections(final ArticleIdentity articleId) {
    return hibernateTemplate.execute(new HibernateCallback<List<ArticleCollection>>() {
      @Override
      public List<ArticleCollection> doInHibernate(Session session) {
        Query query = session.createQuery("" +
            "select c " +
            "from ArticleCollection c join c.articles a " +
            "where a.doi=:doi");
        query.setString("doi", articleId.getKey());
        return query.list();
      }
    });
  }

}
