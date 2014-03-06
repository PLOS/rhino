package org.ambraproject.rhino.service.impl;

import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleList;
import org.ambraproject.models.Journal;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.JournalReadService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.view.JsonWrapper;
import org.ambraproject.rhino.view.KeyedListView;
import org.ambraproject.rhino.view.journal.JournalNonAssocView;
import org.ambraproject.rhino.view.journal.JournalOutputView;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JournalReadServiceImpl extends AmbraService implements JournalReadService {

  @Autowired
  private HibernateTemplate hibernateTemplate;

  @Override
  public void listJournals(ResponseReceiver receiver, MetadataFormat format) throws IOException {
    List<Journal> journals = hibernateTemplate.findByCriteria(journalCriteria());
    KeyedListView<Journal> view = JournalNonAssocView.wrapList(journals);
    serializeMetadata(format, receiver, view);
  }

  @Override
  public void read(ResponseReceiver receiver, String journalKey, MetadataFormat format) throws IOException {
    Journal journal = loadJournal(journalKey);
    serializeMetadata(format, receiver, new JournalOutputView(journal));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void readInTheNewsArticles(ResponseReceiver receiver, final String journalKey, MetadataFormat format)
      throws IOException {
    loadJournal(journalKey);  // just to ensure journalKey is valid

    // A bit of a hack...
    final String key = journalKey.toLowerCase() + "_news";

    // articleList.sortOrder is not exposed as a hibernate property for some reason (although it's a column
    // in the underlying table).  We rely on the fact that sortOrder is part of the primary key for
    // articleListJoinTable to ensure that DOIs are returned in the correct order.  This seems sketchy to
    // me but I don't feel like changing the ambra model class right now.
    ArticleList list = (ArticleList) DataAccessUtils.requiredUniqueResult(hibernateTemplate.findByNamedParam(
        "SELECT al from ArticleList al WHERE al.listCode = :listCode", "listCode", key));
    List<Article> articles = (List<Article>) hibernateTemplate.findByNamedParam(
        "SELECT a from Article a WHERE doi IN (:dois)", "dois", list.getArticleDois());
    if (articles.size() != list.getArticleDois().size()) {
      throw new IllegalStateException("Cannot find all articles for articleList " + key);
    }

    // The results of the last query might not be in the order we want them in...
    Map<String, Article> articleMap = new HashMap<>();
    for (Article article : articles) {
      articleMap.put(article.getDoi(), article);
    }
    List<JsonWrapper<Article>> results = new ArrayList<>(articles.size());
    for (String doi : list.getArticleDois()) {
      results.add(new JsonWrapper<Article>(articleMap.get(doi), "doi", "title"));
    }
    serializeMetadata(format, receiver, results);
  }

  private Journal loadJournal(String journalKey) {
    Journal journal = (Journal) DataAccessUtils.singleResult((List<?>)
        hibernateTemplate.findByCriteria(journalCriteria()
            .add(Restrictions.eq("journalKey", journalKey))
        ));
    if (journal == null) {
      throw new RestClientException("No journal found with key: " + journalKey, HttpStatus.NOT_FOUND);
    }
    return journal;
  }
}
