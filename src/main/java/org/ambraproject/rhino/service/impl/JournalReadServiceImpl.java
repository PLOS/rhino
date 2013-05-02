package org.ambraproject.rhino.service.impl;

import org.ambraproject.models.Journal;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.service.JournalReadService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.view.journal.JournalListView;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.io.IOException;
import java.util.List;

public class JournalReadServiceImpl extends AmbraService implements JournalReadService {

  @Autowired
  private HibernateTemplate hibernateTemplate;

  @Override
  public void read(ResponseReceiver receiver, MetadataFormat format) throws IOException {
    assert format == MetadataFormat.JSON;
    List<Journal> journals = hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Journal.class)
            .setFetchMode("volumes", FetchMode.JOIN)
            .setFetchMode("volumes.issues", FetchMode.JOIN)
    );
    writeJson(receiver, new JournalListView(journals));
  }

}
