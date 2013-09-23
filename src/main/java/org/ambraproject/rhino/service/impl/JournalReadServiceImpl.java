package org.ambraproject.rhino.service.impl;

import org.ambraproject.models.Journal;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.JournalReadService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.view.KeyedListView;
import org.ambraproject.rhino.view.journal.JournalNonAssocView;
import org.ambraproject.rhino.view.journal.JournalOutputView;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.io.IOException;
import java.util.List;

public class JournalReadServiceImpl extends AmbraService implements JournalReadService {

  @Autowired
  private HibernateTemplate hibernateTemplate;

  private static DetachedCriteria journalCriteria() {
    return DetachedCriteria.forClass(Journal.class)
        .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        .addOrder(Order.asc("journalKey"))
        ;
  }

  @Override
  public void listJournals(ResponseReceiver receiver, MetadataFormat format) throws IOException {
    List<Journal> journals = hibernateTemplate.findByCriteria(journalCriteria());
    KeyedListView<JournalNonAssocView> view = JournalNonAssocView.wrapList(journals);
    serializeMetadata(format, receiver, view);
  }

  @Override
  public void read(ResponseReceiver receiver, String journalKey, MetadataFormat format) throws IOException {
    Journal journal = (Journal) DataAccessUtils.singleResult((List<?>)
        hibernateTemplate.findByCriteria(journalCriteria()
            .add(Restrictions.eq("journalKey", journalKey))
            .setFetchMode("volumes", FetchMode.JOIN)
            .setFetchMode("volumes.issues", FetchMode.JOIN)
            .setFetchMode("articleList", FetchMode.JOIN)
        ));
    if (journal == null) {
      throw new RestClientException("No journal found with key: " + journalKey, HttpStatus.NOT_FOUND);
    }
    serializeMetadata(format, receiver, new JournalOutputView(journal));
  }

}
