package org.ambraproject.rhino.service.impl;

import org.ambraproject.models.Journal;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.JournalReadService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.view.journal.JournalKeyView;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
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

  @Override
  public void listKeys(ResponseReceiver receiver, MetadataFormat format) throws IOException {
    assert format == MetadataFormat.JSON;
    List<String> journalKeys = hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Journal.class)
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
            .setProjection(Projections.property("journalKey"))
    );
    writeJson(receiver, new JournalKeyView(journalKeys));
  }

  @Override
  public void read(ResponseReceiver receiver, String journalKey, MetadataFormat format) throws IOException {
    assert format == MetadataFormat.JSON;
    Journal journal = (Journal) DataAccessUtils.singleResult((List<?>)
        hibernateTemplate.findByCriteria(
            DetachedCriteria.forClass(Journal.class)
                .add(Restrictions.eq("journalKey", journalKey))
                .setFetchMode("volumes", FetchMode.JOIN)
                .setFetchMode("volumes.issues", FetchMode.JOIN)
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    if (journal == null) {
      throw new RestClientException("No journal found with key: " + journalKey, HttpStatus.NOT_FOUND);
    }
    writeJson(receiver, journal);
  }

}
