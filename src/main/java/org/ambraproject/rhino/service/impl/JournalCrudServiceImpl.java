/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import org.ambraproject.rhino.identity.IssueIdentifier;
import org.ambraproject.rhino.model.Issue;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.response.CacheableResponse;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.service.IssueCrudService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.view.journal.JournalInputView;
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
import java.util.stream.Collectors;

@SuppressWarnings("JpaQlInspection")
public class JournalCrudServiceImpl extends AmbraService implements JournalCrudService {

  @Autowired
  private HibernateTemplate hibernateTemplate;
  @Autowired
  private IssueCrudService issueCrudService;

  @Override
  public ServiceResponse<Collection<JournalOutputView>> listJournals() throws IOException {
    Collection<Journal> journals = getAllJournals();
    Collection<JournalOutputView> views = journals.stream().map(JournalOutputView::getView).collect(Collectors.toList());
    return ServiceResponse.serveView(views);
  }

  @Override
  public Collection<Journal> getAllJournals() {
    return (List<Journal>) hibernateTemplate
        .execute(session -> session.createCriteria(Journal.class).list());
  }

  @Override
  public CacheableResponse<JournalOutputView> serve(final String journalKey) throws IOException {
    return CacheableResponse.serveEntity(readJournal(journalKey), JournalOutputView::getView);
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
