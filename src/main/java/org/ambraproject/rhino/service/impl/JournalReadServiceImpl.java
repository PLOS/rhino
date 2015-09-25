package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import edu.emory.mathcs.backport.java.util.Collections;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleList;
import org.ambraproject.models.Issue;
import org.ambraproject.models.Journal;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.JournalReadService;
import org.ambraproject.rhino.util.response.EntityCollectionTransceiver;
import org.ambraproject.rhino.util.response.EntityTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.JsonWrapper;
import org.ambraproject.rhino.view.journal.IssueOutputView;
import org.ambraproject.rhino.view.journal.JournalNonAssocView;
import org.ambraproject.rhino.view.journal.JournalOutputView;
import org.ambraproject.rhino.view.journal.VolumeNonAssocView;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class JournalReadServiceImpl extends AmbraService implements JournalReadService {

  @Autowired
  private HibernateTemplate hibernateTemplate;

  @Override
  public Transceiver listJournals() throws IOException {
    return new EntityCollectionTransceiver<Journal>() {
      @Override
      protected Collection<? extends Journal> fetchEntities() {
        return (Collection<? extends Journal>) hibernateTemplate.findByCriteria(journalCriteria());
      }

      @Override
      protected Object getView(Collection<? extends Journal> journals) {
        return JournalNonAssocView.wrapList(journals);
      }
    };
  }

  @Override
  public Transceiver read(final String journalKey) throws IOException {
    return new EntityTransceiver<Journal>() {
      @Override
      protected Journal fetchEntity() {
        return loadJournal(journalKey);
      }

      @Override
      protected Object getView(Journal journal) {
        return new JournalOutputView(journal);
      }
    };
  }

  @Override
  public Transceiver readCurrentIssue(final String journalKey) {
    Preconditions.checkNotNull(journalKey);
    return new Transceiver() {
      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        // Look ahead to the parent volume, and return the max lastModified date between it and the issue.
        return hibernateTemplate.execute(new HibernateCallback<Calendar>() {
          @Override
          public Calendar doInHibernate(Session session) throws HibernateException, SQLException {
            String hql = "select i.lastModified, v.lastModified " +
                "from Journal j, Issue i, Volume v " +
                "where j.journalKey = :journalKey " +
                "and i = j.currentIssue " +
                "and i in elements(v.issues)";
            Query query = session.createQuery(hql);
            query.setParameter("journalKey", journalKey);
            Object[] results = (Object[]) query.uniqueResult();
            if (results == null) return null;

            Date mostRecent = (Date) Collections.max(Arrays.asList(results));
            return copyToCalendar(mostRecent);
          }
        });
      }

      private Object queryJournalProjection(Projection projection) {
        return DataAccessUtils.singleResult((List<?>) hibernateTemplate.findByCriteria(
            DetachedCriteria.forClass(Journal.class)
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
                .add(Restrictions.eq("journalKey", journalKey))
                .setProjection(Preconditions.checkNotNull(projection))
        ));
      }

      @Override
      protected Object getData() throws IOException {
        final Issue issue = (Issue) queryJournalProjection(Projections.property("currentIssue"));
        if (issue == null) {
          // Neither failure case is expected, so indulge in an extra query to give a more informative message.
          Long count = (Long) queryJournalProjection(Projections.rowCount());
          String message = (count == 0L) ? journalNotFoundMessage(journalKey)
              : String.format("Journal found with key \"%s\", but its current issue is not set", journalKey);
          throw new RestClientException(message, HttpStatus.BAD_REQUEST);
        }

        Object[] parentVolumeResults = hibernateTemplate.execute(new HibernateCallback<Object[]>() {
          @Override
          public Object[] doInHibernate(Session session) throws HibernateException, SQLException {
            String hql = "select volumeUri, displayName, imageUri, title, description " +
                "from Volume where :issue in elements(issues)";
            Query query = session.createQuery(hql);
            query.setParameter("issue", issue);
            return (Object[]) query.uniqueResult();
          }
        });
        VolumeNonAssocView parentVolumeView = (parentVolumeResults == null) ? null
            : VolumeNonAssocView.fromArray(parentVolumeResults);

        return new IssueOutputView(issue, parentVolumeView);
      }
    };
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Transceiver readInTheNewsArticles(final String journalKey)
      throws IOException {
    return new Transceiver() {
      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null; // Unsupported on this operation for now
      }

      @Override
      protected List<JsonWrapper<Article>> getData() throws IOException {
        loadJournal(journalKey);  // just to ensure journalKey is valid

        // A bit of a hack...
        final String key = journalKey.toLowerCase() + "_news";

        ArticleList list = (ArticleList) DataAccessUtils.requiredUniqueResult(hibernateTemplate.findByNamedParam(
            "SELECT al from ArticleList al WHERE al.listCode = :listCode", "listCode", key));
        List<Article> articles = list.getArticles();
        if (articles.isEmpty()) {
          return ImmutableList.of();
        }

        List<JsonWrapper<Article>> results = new ArrayList<>(articles.size());
        for (Article article : articles) {
          results.add(new JsonWrapper<Article>(article, "doi", "title"));
        }
        return results;
      }
    };
  }

  private static String journalNotFoundMessage(String journalKey) {
    return "No journal found with key: " + journalKey;
  }

  private Journal loadJournal(String journalKey) {
    Journal journal = (Journal) DataAccessUtils.singleResult((List<?>)
        hibernateTemplate.findByCriteria(journalCriteria()
                .add(Restrictions.eq("journalKey", journalKey))
        ));
    if (journal == null) {
      throw new RestClientException(journalNotFoundMessage(journalKey), HttpStatus.NOT_FOUND);
    }
    return journal;
  }
}
