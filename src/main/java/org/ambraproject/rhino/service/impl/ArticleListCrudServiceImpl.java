package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.ambraproject.models.AmbraEntity;
import org.ambraproject.models.ArticleList;
import org.ambraproject.models.Journal;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.ArticleListIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleListCrudService;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ArticleListCrudServiceImpl extends AmbraService implements ArticleListCrudService {

  @Override
  public ArticleList create(ArticleListIdentity identity, String displayName, Set<ArticleIdentity> articleIds) {
    ArticleList list = new ArticleList();
    list.setListCode(identity.getListCode());
    list.setDisplayName(displayName);

    list.setArticleDois(getArticleKeys(articleIds));

    Journal journal = (Journal) DataAccessUtils.uniqueResult(hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Journal.class)
            .add(Restrictions.eq("journalKey", identity.getJournalKey()))));
    if (journal == null) {
      throw new RestClientException("Journal not found: " + identity.getJournalKey(), HttpStatus.BAD_REQUEST);
    }
    List<ArticleList> journalLists = journal.getArticleList();
    if (journalLists == null) {
      journal.setArticleList(journalLists = new ArrayList<>(1));
    }
    journalLists.add(list);
    hibernateTemplate.update(journal);

    return list;
  }

  private ArticleList getArticleList(final ArticleListIdentity identity) {
    return DataAccessUtils.uniqueResult(hibernateTemplate.execute(new HibernateCallback<List<ArticleList>>() {
      @Override
      public List<ArticleList> doInHibernate(Session session) throws HibernateException, SQLException {
        Query query = session.createQuery("" +
            "select l " +
            "from Journal j inner join j.articleList l " +
            "where (j.journalKey=:journalKey) and (l.listCode=:listCode)");
        query.setString("listCode", identity.getListCode());
        query.setString("journalKey", identity.getJournalKey());
        return query.list();
      }
    }));
  }

  private static RuntimeException nonexistentList(ArticleListIdentity identity) {
    return new RestClientException("Link does not exist: " + identity, HttpStatus.NOT_FOUND);
  }

  @Override
  public ArticleList update(ArticleListIdentity identity, String displayName, Set<ArticleIdentity> articleIds) {
    ArticleList list = getArticleList(identity);
    if (list == null) {
      throw nonexistentList(identity);
    }

    if (displayName != null) {
      list.setDisplayName(displayName);
    }

    if (articleIds != null) {
      Preconditions.checkArgument(!articleIds.isEmpty());
      List<String> newDois = getArticleKeys(articleIds);
      List<String> oldDois = list.getArticleDois();
      oldDois.clear();
      oldDois.addAll(newDois);
    }

    hibernateTemplate.update(list);
    return list;
  }

  /**
   * Validate that all of a set of named articles exist. Convert into a form that can be persisted in an {@link
   * org.ambraproject.models.ArticleList}.
   *
   * @param articleIds a set of article IDs
   * @return the article IDs, formatted for storage, if all exist
   * @throws RestClientException if not every article ID belongs to an existing article
   */
  private List<String> getArticleKeys(Set<ArticleIdentity> articleIds) {
    if (articleIds.isEmpty()) return ImmutableList.of();
    final List<String> articleKeys = new ArrayList<>(articleIds.size());
    for (ArticleIdentity articleId : articleIds) {
      articleKeys.add(articleId.getKey());
    }

    List<String> foundDois = hibernateTemplate.execute(new HibernateCallback<List<String>>() {
      @Override
      public List<String> doInHibernate(Session session) throws HibernateException, SQLException {
        Query query = session.createQuery("select distinct doi from Article where doi in :articleKeys");
        query.setParameterList("articleKeys", articleKeys);
        return query.list();
      }
    }); // make sure not to return foundDois, because it doesn't have the same order as articleKeys
    if (foundDois.size() < articleKeys.size()) {
      throw new RestClientException(buildMissingArticleMessage(foundDois, articleKeys), HttpStatus.NOT_FOUND);
    }

    return articleKeys;
  }

  private static String buildMissingArticleMessage(Collection<String> foundArticleKeys, Collection<String> requestedArticleKeys) {
    Collection<String> missingKeys = Sets.difference(
        ImmutableSet.copyOf(requestedArticleKeys), ImmutableSet.copyOf(foundArticleKeys));
    return "Articles not found with DOIs: " + new Gson().toJson(missingKeys);
  }

  @Override
  public Transceiver read(final ArticleListIdentity identity) {
    return new EntityTransceiver() {
      @Override
      protected ArticleList fetchEntity() {
        ArticleList result = getArticleList(identity);
        if (result == null) {
          throw nonexistentList(identity);
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
  public Collection<ArticleList> getContainingLists(final ArticleIdentity articleId) {
    return hibernateTemplate.execute(new HibernateCallback<List<ArticleList>>() {
      @Override
      public List<ArticleList> doInHibernate(Session session) {
        Query query = session.createQuery("from ArticleList l join l.articleDois d where d=:doi");
        query.setString("doi", articleId.getKey());
        return query.list();
      }
    });
  }

}
