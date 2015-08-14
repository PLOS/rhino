package org.ambraproject.rhino.service.impl;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.ambraproject.models.AmbraEntity;
import org.ambraproject.models.Article;
import org.ambraproject.models.Journal;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.ArticleLinkIdentity;
import org.ambraproject.rhino.model.ArticleLink;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleLinkCrudService;
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

public class ArticleLinkCrudServiceImpl extends AmbraService implements ArticleLinkCrudService {

  @Override
  public ArticleLink create(ArticleLinkIdentity identity, String title, Set<ArticleIdentity> articleIds) {
    ArticleLink link = new ArticleLink();
    link.setLinkType(identity.getLinkType());
    link.setTarget(identity.getTarget());
    link.setTitle(title);

    Journal journal = (Journal) DataAccessUtils.uniqueResult(hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Journal.class)
            .add(Restrictions.eq("journalKey", identity.getJournalKey()))));
    if (journal == null) {
      throw new RestClientException("Journal not found: " + identity.getJournalKey(), HttpStatus.BAD_REQUEST);
    }
    link.setJournal(journal);

    List<Article> articles = fetchArticles(articleIds);
    link.setArticles(articles);

    hibernateTemplate.persist(link);
    return link;
  }

  private ArticleLink getArticleLink(final ArticleLinkIdentity identity) {
    return DataAccessUtils.uniqueResult(hibernateTemplate.execute(new HibernateCallback<List<ArticleLink>>() {
      @Override
      public List<ArticleLink> doInHibernate(Session session) throws HibernateException, SQLException {
        Query query = session.createQuery("" +
            "from ArticleLink l " +
            "where l.linkType=:linkType and l.target=:target and l.journal.journalKey=:journalKey");
        query.setString("linkType", identity.getLinkType());
        query.setString("target", identity.getTarget());
        query.setString("journalKey", identity.getJournalKey());
        return query.list();
      }
    }));
  }

  private static RuntimeException nonexistentLink(ArticleLinkIdentity identity) {
    return new RestClientException("Link does not exist: " + identity, HttpStatus.NOT_FOUND);
  }

  @Override
  public ArticleLink update(ArticleLinkIdentity identity,
                            Optional<String> title, Optional<? extends Set<ArticleIdentity>> articleIds) {
    ArticleLink link = getArticleLink(identity);
    if (link == null) {
      throw nonexistentLink(identity);
    }

    if (title.isPresent()) {
      link.setTitle(title.get());
    }

    if (articleIds.isPresent()) {
      List<Article> newArticles = fetchArticles(articleIds.get());
      List<Article> oldArticles = link.getArticles();
      oldArticles.clear();
      oldArticles.addAll(newArticles);
    }

    hibernateTemplate.update(link);
    return link;
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
  public Transceiver read(final ArticleLinkIdentity identity) {
    return new EntityTransceiver() {
      @Override
      protected ArticleLink fetchEntity() {
        ArticleLink result = getArticleLink(identity);
        if (result == null) {
          throw nonexistentLink(identity);
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
  public Collection<ArticleLink> getAssociatedLinks(final ArticleIdentity articleId) {
    return hibernateTemplate.execute(new HibernateCallback<List<ArticleLink>>() {
      @Override
      public List<ArticleLink> doInHibernate(Session session) {
        Query query = session.createQuery("" +
            "select l " +
            "from ArticleLink l join l.articles a " +
            "where a.doi=:doi");
        query.setString("doi", articleId.getKey());
        return query.list();
      }
    });
  }

}
