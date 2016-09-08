package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import org.ambraproject.rhino.identity.IssueIdentifier;
import org.ambraproject.rhino.model.Issue;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.util.response.EntityCollectionTransceiver;
import org.ambraproject.rhino.util.response.EntityTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.journal.IssueOutputView;
import org.ambraproject.rhino.view.journal.JournalInputView;
import org.ambraproject.rhino.view.journal.JournalNonAssocView;
import org.ambraproject.rhino.view.journal.JournalOutputView;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("JpaQlInspection")
public class JournalCrudServiceImpl extends AmbraService implements JournalCrudService {

  @Autowired
  private HibernateTemplate hibernateTemplate;
  @Autowired
  private IssueCrudService issueCrudService;
  @Autowired
  private IssueOutputView.Factory issueOutputViewFactory;
  @Autowired
  private JournalOutputView.Factory journalOutputViewFactory;

  @Override
  public Transceiver listJournals() throws IOException {
    return new EntityCollectionTransceiver<Journal>() {
      @Override
      protected Collection<? extends Journal> fetchEntities() {
        return getAllJournals();
      }

      @Override
      protected Object getView(Collection<? extends Journal> journals) {
        return JournalNonAssocView.wrapList(journals);
      }
    };
  }

  private Collection<? extends Journal> getAllJournals() {
    return (List<Journal>) hibernateTemplate
        .execute(session -> session.createCriteria(Journal.class).list());
  }

  @Override
  public Transceiver serve(final String journalKey) throws IOException {
    return new EntityTransceiver<Journal>() {
      @Override
      protected Journal fetchEntity() {
        return readJournal(journalKey);
      }

      @Override
      protected Object getView(Journal journal) {
        return journalOutputViewFactory.getView(journal);
      }
    };
  }

  @Override
  public void update(String journalKey, JournalInputView input) {
    Preconditions.checkNotNull(input);
    Journal journal = readJournal(journalKey);
    hibernateTemplate.update(applyInput(journal, input));
  }

  private Journal applyInput(Journal journal, JournalInputView input) {
    String currentIssueDoi = input.getCurrentIssueDoi();
    if (currentIssueDoi != null) {
      Issue currentIssue = issueCrudService.readIssue(IssueIdentifier.create(currentIssueDoi));
      validateIssueInJournal(currentIssue, journal);
      journal.setCurrentIssue(currentIssue);
    }
    return journal;
  }

  private Issue validateIssueInJournal(Issue issue, Journal journal) {

    Object results = hibernateTemplate.execute((Session session) -> {
      String hql = "from Journal j, Issue i, Volume v " +
          "where j.journalId = :journalId " +
          "and i.issueId = :issueId " +
          "and v in elements(j.volumes) " +
          "and i in elements(v.issues)";
      Query query = session.createQuery(hql);
      query.setParameter("journalId", journal.getJournalId());
      query.setParameter("issueId", issue.getIssueId());
      return query.uniqueResult();
    });
    if (results != null) {
      return issue;
    } else {
      throw new RestClientException("Issue with DOI " + issue.getDoi() +
          " not found in journal with key " + journal.getJournalKey(), HttpStatus.BAD_REQUEST);
    }
  }

  @Override
  public Optional<Journal> getJournal(String journalKey) {
    return Optional.ofNullable(hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM Journal j WHERE j.journalKey = :journalKey ");
      query.setParameter("journalKey", journalKey);
      return (Journal) query.uniqueResult();
    }));
  }

  @Override
  public Journal readJournal(String journalKey) {
    return getJournal(journalKey).orElseThrow(() -> new RestClientException("No journal found with key: " + journalKey, HttpStatus.NOT_FOUND));
  }

  @Override
  public Optional<Journal> getJournalByEissn(String eIssn) {
    return Optional.ofNullable(hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM Journal j WHERE j.eIssn = :eIssn");
      query.setParameter("eIssn", eIssn);
      return (Journal) query.uniqueResult();
    }));
  }

  @Override
  public Journal readJournalByEissn(String eIssn) {
    return getJournalByEissn(eIssn).orElseThrow(() ->
        new RestClientException("No journal found with eIssn: " + eIssn, HttpStatus.NOT_FOUND));
  }

  @Override
  public Journal readJournalByVolume(Volume volume) {
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("FROM Journal j WHERE :volume IN ELEMENTS(volumes)");
      query.setParameter("volume", volume);
      return (Journal) query.uniqueResult();
    });
  }
}
