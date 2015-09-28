package org.ambraproject.rhino.service.impl;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.ambraproject.models.AmbraEntity;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleList;
import org.ambraproject.models.Journal;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.ArticleListIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleListCrudService;
import org.ambraproject.rhino.util.response.EntityTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.article.DeepArticleListView;
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
  public ArticleList create(ArticleListIdentity identity, String displayName, Set<ArticleIdentity> articleIds) {
    ArticleList list = new ArticleList();
    list.setListType(identity.getListType().orNull());
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

    return list;
  }

  private ArticleList getArticleList(final ArticleListIdentity identity) {
    ArticleList result = DataAccessUtils.uniqueResult(hibernateTemplate.execute(new HibernateCallback<List<ArticleList>>() {
      @Override
      public List<ArticleList> doInHibernate(Session session) throws HibernateException, SQLException {
        Optional<String> listType = identity.getListType();
        Query query = session.createQuery("" +
            "select l " +
            "from Journal j inner join j.articleLists l " +
            "where (j.journalKey=:journalKey) and (l.listCode=:listCode) and " +
            (listType.isPresent() ? "(l.listType=:listType)" : "(l.listType is null)"));
        query.setString("journalKey", identity.getJournalKey());
        query.setString("listCode", identity.getListCode());
        if (listType.isPresent()) {
          query.setString("listType", identity.getListType().get());
        }
        return query.list();
      }
    }));
    if (result == null) {
      throw nonexistentList(identity);
    }
    return result;
  }

  private static RuntimeException nonexistentList(ArticleListIdentity identity) {
    return new RestClientException("List does not exist: " + identity, HttpStatus.NOT_FOUND);
  }

  @Override
  public ArticleList update(ArticleListIdentity identity, Optional<String> displayName,
                            Optional<? extends Set<ArticleIdentity>> articleIds) {
    ArticleList list = getArticleList(identity);

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
    return list;
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
  public Transceiver read(final ArticleListIdentity identity, boolean includeArticleMetadata) {
    return includeArticleMetadata ? readDeep(identity) : readShallow(identity);
  }

  private Transceiver readShallow(final ArticleListIdentity identity) {
    return new EntityTransceiver() {
      @Override
      protected ArticleList fetchEntity() {
        return getArticleList(identity);
      }

      @Override
      protected Object getView(AmbraEntity entity) {
        return entity;
      }
    };
  }

  private Transceiver readDeep(ArticleListIdentity identity) {
    // "Last modified" is the latest timestamp among all articles in the collection.
    // For now, don't bother trying to read it, and always return data. Could revisit this if necessary.

    final ArticleList articleList = getArticleList(identity);
    return new Transceiver() {
      @Override
      protected Object getData() throws IOException {
        return new DeepArticleListView(articleList);
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  @Override
  public Collection<ArticleListView> findContainingLists(final ArticleIdentity articleId) {
    return hibernateTemplate.execute(new HibernateCallback<Collection<ArticleListView>>() {
      @Override
      public Collection<ArticleListView> doInHibernate(Session session) {
        Query query = session.createQuery("" +
            "select j.journalKey, l.listType, l.listCode, l.displayName " +
            "from Journal j join j.articleLists l join l.articles a " +
            "where a.doi=:doi");
        query.setString("doi", articleId.getKey());
        List<Object[]> results = query.list();

        Collection<ArticleListView> views = new ArrayList<>(results.size());
        for (Object[] result : results) {
          String journalKey = (String) result[0];
          Optional<String> listType = Optional.fromNullable((String) result[1]);
          String listCode = (String) result[2];
          String displayName = (String) result[3];

          ArticleListIdentity identity = new ArticleListIdentity(listType, journalKey, listCode);
          views.add(new ArticleListView(identity, displayName));
        }
        return views;
      }
    });
  }

}
