package org.ambraproject.rhino.service.impl;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleListIdentity;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleList;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.service.ArticleListCrudService;
import org.ambraproject.rhino.view.journal.ArticleListView;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ArticleListCrudServiceImpl extends AmbraService implements ArticleListCrudService {

  @Autowired
  private ArticleListView.Factory articleListViewFactory;

  private static Query queryFor(Session hibernateSession, String selectClause, ArticleListIdentity identity) {
    Query query = hibernateSession.createQuery(selectClause +
        " from Journal j inner join j.articleLists l " +
        "where (j.journalKey=:journalKey) and (l.listKey=:listKey) and (l.listType=:listType)");
    query.setString("journalKey", identity.getJournalKey());
    query.setString("listKey", identity.getKey());
    query.setString("listType", identity.getType());
    return query;
  }

  private boolean listExists(final ArticleListIdentity identity) {
    long count = hibernateTemplate.execute(new HibernateCallback<Long>() {
      @Override
      public Long doInHibernate(Session session) throws HibernateException, SQLException {
        Query query = queryFor(session, "select count(*)", identity);
        return (Long) query.uniqueResult();
      }
    });
    return count > 0L;
  }

  @Override
  public ArticleListView create(ArticleListIdentity identity, String displayName, Set<ArticleIdentifier> articleIds) {
    if (listExists(identity)) {
      throw new RestClientException("List already exists: " + identity, HttpStatus.BAD_REQUEST);
    }

    ArticleList list = new ArticleList();
    list.setListType(identity.getType());
    list.setListKey(identity.getKey());
    list.setDisplayName(displayName);

    list.setArticles(fetchArticles(articleIds));

    Journal journal = (Journal) DataAccessUtils.uniqueResult(hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Journal.class)
            .add(Restrictions.eq("journalKey", identity.getJournalKey()))));
    if (journal == null) {
      throw new RestClientException("Journal not found: " + identity.getJournalKey(), HttpStatus.BAD_REQUEST);
    }
    Collection<ArticleList> journalLists = journal.getArticleLists();
    if (journalLists == null) {
      journal.setArticleLists(journalLists = new ArrayList<>(1));
    }
    journalLists.add(list);
    hibernateTemplate.update(journal);

    return articleListViewFactory.getView(list, journal.getJournalKey());
  }

  private ArticleListView getArticleList(final ArticleListIdentity identity) {
    Object[] result = DataAccessUtils.uniqueResult(hibernateTemplate.execute(new HibernateCallback<List<Object[]>>() {
      @Override
      public List<Object[]> doInHibernate(Session session) throws HibernateException, SQLException {
        Query query = queryFor(session, "select j.journalKey, l", identity);
        return query.list();
      }
    }));
    if (result == null) {
      throw nonexistentList(identity);
    }

    String journalKey = (String) result[0];
    ArticleList articleList = (ArticleList) result[1];
    return articleListViewFactory.getView(articleList, journalKey);
  }

  private static RuntimeException nonexistentList(ArticleListIdentity identity) {
    return new RestClientException("List does not exist: " + identity, HttpStatus.NOT_FOUND);
  }

  @Override
  public ArticleListView update(ArticleListIdentity identity, Optional<String> displayName,
                                Optional<? extends Set<ArticleIdentifier>> articleIds) {
    ArticleListView listView = getArticleList(identity);
    ArticleList list = listView.getArticleList();

    if (displayName.isPresent()) {
      list.setDisplayName(displayName.get());
    }

    if (articleIds.isPresent()) {
      List<Article> newArticles = fetchArticles(articleIds.get());
      List<Article> oldArticles = list.getArticles();
      oldArticles.clear();
      oldArticles.addAll(newArticles);
    }

    hibernateTemplate.update(list);
    return listView;
  }

  /**
   * Fetch all articles with the given IDs, in the same iteration error.
   *
   * @param articleIds a set of article IDs
   * @return the articles in the same order, if all exist
   * @throws RestClientException if not every article ID belongs to an existing article
   */
  private List<Article> fetchArticles(Set<ArticleIdentifier> articleIds) {
    if (articleIds.isEmpty()) return ImmutableList.of();
    final Map<String, Integer> articleKeys = new HashMap<>();
    int i = 0;
    for (ArticleIdentifier articleId : articleIds) {
      articleKeys.put(articleId.getDoiName(), i++);
    }

    List<Article> articles = (List<Article>) hibernateTemplate.findByNamedParam(
        "from Article where doi in :articleKeys", "articleKeys", articleKeys.keySet());
    if (articles.size() < articleKeys.size()) {
      throw new RestClientException(buildMissingArticleMessage(articles, articleKeys.keySet()), HttpStatus.NOT_FOUND);
    }

    Collections.sort(articles, new Comparator<Article>() {
      @Override
      public int compare(Article o1, Article o2) {
        // We expect the error check above to guarantee that both values will be found in the map
        int i1 = articleKeys.get(o1.getDoi());
        int i2 = articleKeys.get(o2.getDoi());
        return i1 - i2;
      }
    });

    return articles;
  }

  private static String buildMissingArticleMessage(Collection<Article> foundArticles, Collection<String> requestedArticleKeys) {
    ImmutableSet.Builder<String> foundArticleKeys = ImmutableSet.builder();
    for (Article foundArticle : foundArticles) {
      foundArticleKeys.add(foundArticle.getDoi());
    }

    Collection<String> missingKeys = Sets.difference(
        ImmutableSet.copyOf(requestedArticleKeys), foundArticleKeys.build());
    missingKeys = new ArrayList<>(missingKeys); // coerce to a type that Gson can handle
    return "Articles not found with DOIs: " + new Gson().toJson(missingKeys);
  }

  @Override
  public ServiceResponse<ArticleListView> read(final ArticleListIdentity identity) {
    return ServiceResponse.serveView(getArticleList(identity));
  }

  private Collection<ArticleListView> asArticleListViews(List<Object[]> results) {
    Collection<ArticleListView> views = new ArrayList<>(results.size());
    for (Object[] result : results) {
      String journalKey = (String) result[0];
      ArticleList articleList = (ArticleList) result[1];
      views.add(articleListViewFactory.getView(articleList, journalKey));
    }
    return views;
  }

  @Override
  public ServiceResponse<Collection<ArticleListView>> readAll(final Optional<String> listType, final Optional<String> journalKey) {
    if (!listType.isPresent() && journalKey.isPresent()) {
      throw new IllegalArgumentException();
    }
    List<Object[]> result = hibernateTemplate.execute(new HibernateCallback<List<Object[]>>() {
      @Override
      public List<Object[]> doInHibernate(Session session) throws HibernateException, SQLException {
        StringBuilder queryString = new StringBuilder(125)
            .append("select j.journalKey, l from Journal j inner join j.articleLists l");
        if (listType.isPresent()) {
          queryString.append(" where (l.listType=:listType)");
          if (journalKey.isPresent()) {
            queryString.append(" and (j.journalKey=:journalKey)");
          }
        }

        Query query = session.createQuery(queryString.toString());
        if (listType.isPresent()) {
          query.setParameter("listType", listType.get());
          if (journalKey.isPresent()) {
            query.setParameter("journalKey", journalKey.get());
          }
        }

        return query.list();
      }
    });
    Collection<ArticleListView> views = asArticleListViews(result);
    return ServiceResponse.serveView(views);
  }

  private Collection<ArticleListView> findContainingLists(final ArticleIdentifier articleId) {
    return hibernateTemplate.execute(new HibernateCallback<Collection<ArticleListView>>() {
      @Override
      public Collection<ArticleListView> doInHibernate(Session session) {
        Query query = session.createQuery("" +
            "select j.journalKey, l " +
            "from Journal j join j.articleLists l join l.articles a " +
            "where a.doi=:doi");
        query.setString("doi", articleId.getDoiName());
        List<Object[]> results = query.list();
        return asArticleListViews(results);
      }
    });
  }

  @Override
  public ServiceResponse<Collection<ArticleListView>> readContainingLists(final ArticleIdentifier articleId) {
    return ServiceResponse.serveView(findContainingLists(articleId));
  }

}
