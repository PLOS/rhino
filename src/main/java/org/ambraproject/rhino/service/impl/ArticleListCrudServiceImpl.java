package org.ambraproject.rhino.service.impl;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleList;
import org.ambraproject.models.Journal;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.ArticleListIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleListCrudService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.journal.ArticleListView;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ArticleListCrudServiceImpl extends AmbraService implements ArticleListCrudService {

  @Override
  public ArticleListView create(ArticleListIdentity identity, String displayName, Set<ArticleIdentity> articleIds) {
    ArticleList list = new ArticleList();
    list.setListType(identity.getListType());
    list.setListCode(identity.getListCode());
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
    journalLists.add(list); // TODO: Check that new identity doesn't collide
    hibernateTemplate.update(journal);

    return new ArticleListView(journal.getJournalKey(), list);
  }

  private ArticleListView getArticleList(final ArticleListIdentity identity) {
    Object[] result = DataAccessUtils.uniqueResult(hibernateTemplate.execute(new HibernateCallback<List<Object[]>>() {
      @Override
      public List<Object[]> doInHibernate(Session session) throws HibernateException, SQLException {
        Query query = session.createQuery("" +
            "select j.journalKey, l " +
            "from Journal j inner join j.articleLists l " +
            "where (j.journalKey=:journalKey) and (l.listCode=:listCode) and (l.listType=:listType)");
        query.setString("journalKey", identity.getJournalKey());
        query.setString("listCode", identity.getListCode());
        query.setString("listType", identity.getListType());
        return query.list();
      }
    }));
    if (result == null) {
      throw nonexistentList(identity);
    }

    String journalKey = (String) result[0];
    ArticleList articleList = (ArticleList) result[1];
    return new ArticleListView(journalKey, articleList);
  }

  private static RuntimeException nonexistentList(ArticleListIdentity identity) {
    return new RestClientException("List does not exist: " + identity, HttpStatus.NOT_FOUND);
  }

  @Override
  public ArticleListView update(ArticleListIdentity identity, Optional<String> displayName,
                                Optional<? extends Set<ArticleIdentity>> articleIds) {
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
  private List<Article> fetchArticles(Set<ArticleIdentity> articleIds) {
    if (articleIds.isEmpty()) return ImmutableList.of();
    final Map<String, Integer> articleKeys = new HashMap<>();
    int i = 0;
    for (ArticleIdentity articleId : articleIds) {
      articleKeys.put(articleId.getKey(), i++);
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
    return "Articles not found with DOIs: " + new Gson().toJson(missingKeys);
  }

  @Override
  public Transceiver read(final ArticleListIdentity identity) {
    return new Transceiver() {
      @Override
      protected ArticleListView getData() throws IOException {
        return getArticleList(identity);
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  private static Collection<ArticleListView> asArticleListViews(List<Object[]> results) {
    Collection<ArticleListView> views = new ArrayList<>(results.size());
    for (Object[] result : results) {
      String journalKey = (String) result[0];
      ArticleList articleList = (ArticleList) result[1];
      views.add(new ArticleListView(journalKey, articleList));
    }
    return views;
  }

  @Override
  public Transceiver readAll(final Optional<String> listType, final Optional<String> journalKey) {
    if (!listType.isPresent() && journalKey.isPresent()) {
      throw new IllegalArgumentException();
    }
    return new Transceiver() {
      @Override
      protected Object getData() throws IOException {
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
        return asArticleListViews(result);
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  private Collection<ArticleListView> findContainingLists(final ArticleIdentity articleId) {
    return hibernateTemplate.execute(new HibernateCallback<Collection<ArticleListView>>() {
      @Override
      public Collection<ArticleListView> doInHibernate(Session session) {
        Query query = session.createQuery("" +
            "select j.journalKey, l " +
            "from Journal j join j.articleLists l join l.articles a " +
            "where a.doi=:doi");
        query.setString("doi", articleId.getKey());
        List<Object[]> results = query.list();
        return asArticleListViews(results);
      }
    });
  }

  @Override
  public Transceiver readContainingLists(final ArticleIdentity articleId) {
    return new Transceiver() {
      @Override
      protected Collection<ArticleListView> getData() throws IOException {
        return findContainingLists(articleId);
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        // TODO: Use maximum lastModified from findContainingLists(articleId)?
        // Maybe we want to adapt EntityCollectionTransceiver to extract the entity (ArticleList) from a wrapping view
        return null;
      }
    };
  }

}
