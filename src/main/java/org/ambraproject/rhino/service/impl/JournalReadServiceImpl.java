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
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
    List<Article> articles = hibernateTemplate.execute(new HibernateCallback<List<Article>>() {
      @Override
      public List<Article> doInHibernate(Session session) throws HibernateException, SQLException {

        // Since ArticleList's foreign key into Article is doi, and not articleID, it doesn't appear
        // possible to do this in a single HQL query.
        Query q = session.createQuery("SELECT al from ArticleList al WHERE al.listCode = :listCode");
        q.setParameter("listCode", key);
        List<ArticleList> articleLists = q.list();
        if (articleLists.size() == 0) {
          throw new RestClientException(journalKey + " has no in the news article list", HttpStatus.NOT_FOUND);
        } else if (articleLists.size() > 1) {
          throw new IllegalStateException(String.format("Expected 1 articleList for %s, got %d",
              journalKey, articleLists.size()));
        }
        q = session.createQuery("SELECT a from Article a WHERE doi IN (:dois)");
        q.setParameterList("dois", articleLists.get(0).getArticleDois());
        return q.list();
      }
    });

    List<JsonWrapper<Article>> results = new ArrayList<>(articles.size());
    for (Article article : articles) {
      results.add(new JsonWrapper<Article>(article, "doi", "title"));
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
