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

import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Syndication;
import org.ambraproject.rhino.model.SyndicationStatus;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.service.MessageSender;
import org.ambraproject.rhino.service.SyndicationCrudService;
import org.ambraproject.rhino.view.article.ArticleJsonNames;
import org.ambraproject.rhino.view.article.SyndicationOutputView;
import org.hibernate.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manage the syndication process, including creating and updating Syndication objects, as well as pushing syndication
 * messages to a message queue.
 *
 * @author Scott Sterling
 * @author Joe Osowski
 */
@SuppressWarnings({"JpaQlInspection"})
public class SyndicationCrudServiceImpl extends AmbraService implements SyndicationCrudService {
  private static final Logger log = LoggerFactory.getLogger(SyndicationCrudServiceImpl.class);

  @Autowired
  private MessageSender messageSender;

  @Autowired
  private RuntimeConfiguration runtimeConfiguration;

  @Autowired
  private JournalCrudService journalService;

  @Autowired
  private ArticleCrudService articleCrudService;

  @Override
  @SuppressWarnings("unchecked")
  public Syndication getSyndication(final ArticleRevisionIdentifier revisionId,
                                    final String syndicationTargetQueue) {
    ArticleRevision articleRevision = articleCrudService.readRevision(revisionId);
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "FROM Syndication s " +
          "WHERE s.targetQueue = :targetQueue " +
          "AND s.articleRevision = :articleRevision");
      query.setParameter(ArticleJsonNames.SYNDICATION_TARGET, syndicationTargetQueue);
      query.setParameter("articleRevision", articleRevision);
      return (Syndication) query.uniqueResult();
    });
  }

  @Transactional(readOnly = true)
  @SuppressWarnings("unchecked")
  @Override
  public List<Syndication> getSyndications(ArticleRevisionIdentifier revisionId) {
    ArticleRevision articleRevision = articleCrudService.readRevision(revisionId);
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "FROM Syndication s " +
          "WHERE s.articleRevision = :articleRevision");
      query.setParameter("articleRevision", articleRevision);
      return (List<Syndication>) query.list();
    });
  }

  @Transactional(rollbackFor = {Throwable.class})
  @Override
  public Syndication updateSyndication(final ArticleRevisionIdentifier revisionId,
      final String syndicationTargetQueue, final String status, final String errorMessage) {
    Syndication syndication = getSyndication(revisionId, syndicationTargetQueue);
    if (syndication == null) {
      throw new RuntimeException("No such syndication for doi " + revisionId
          + " and target " + syndicationTargetQueue);
    }
    syndication.setStatus(status);
    syndication.setErrorMessage(errorMessage);
    hibernateTemplate.update(syndication);

    return syndication;
  }

  @Override
  public Syndication createSyndication(ArticleRevisionIdentifier revisionId, String syndicationTargetQueue) {
    ArticleRevision articleVersion = articleCrudService.readRevision(revisionId);
    Syndication syndication = new Syndication(articleVersion, syndicationTargetQueue);
    syndication.setStatus(SyndicationStatus.PENDING.getLabel());
    syndication.setSubmissionCount(0);
    hibernateTemplate.save(syndication);
    return syndication;
  }

  @Transactional
  @Override
  public Collection<Syndication> getSyndications(final String journalKey, final List<String> statuses) {
    int numDaysInPast = runtimeConfiguration.getQueueConfiguration().getSyndicationRange();

    LocalDate startDate = LocalDate.now().minus(numDaysInPast, ChronoUnit.DAYS);
    Instant startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

    final Journal journal = journalService.readJournal(journalKey);

    if (journal == null) {
      throw new RuntimeException("Could not find journal for journal key: " + journalKey);
    }

    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "SELECT s " +
          "FROM Syndication s " +
          "JOIN s.articleVersion av " +
          "JOIN av.journals j " +
          "WHERE j.journalKey = :journalKey " +
          "AND s.status in (:statuses)" +
          "AND s.lastModified > :startTime");
      query.setParameter("journalKey", journalKey);
      query.setParameterList("statuses", statuses);
      query.setDate("startTime", Date.from(startTime));
      return (Collection<Syndication>) query.list();
    });
  }

  @Override
  public ServiceResponse<Collection<SyndicationOutputView>> readSyndications(String journalKey, List<String> statuses) throws IOException {
    Collection<Syndication> syndications = getSyndications(journalKey, statuses);
    Collection<SyndicationOutputView> views = syndications.stream().map(SyndicationOutputView::createSyndicationView).collect(Collectors.toList());
    return ServiceResponse.serveView(views);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @Override
  public Syndication syndicate(ArticleRevisionIdentifier revisionId, String syndicationTargetQueue) {
      ArticleRevision articleVersion = articleCrudService.readRevision(revisionId);

    Syndication syndication = getSyndication(revisionId, syndicationTargetQueue);
    if (syndication == null) {
      syndication = new Syndication(articleVersion, syndicationTargetQueue);
      syndication.setStatus(SyndicationStatus.IN_PROGRESS.getLabel());
      syndication.setSubmissionCount(1);
      syndication.setLastSubmitTimestamp(new Date());
      hibernateTemplate.save(syndication);
    } else {
      syndication.setStatus(SyndicationStatus.IN_PROGRESS.getLabel());
      syndication.setSubmissionCount(syndication.getSubmissionCount() + 1);
      syndication.setLastSubmitTimestamp(new Date());
      hibernateTemplate.update(syndication);
    }

    try {
      messageSender.sendBody(syndicationTargetQueue, createBody(revisionId));
      log.info("Successfully sent a Message to plos-queue for {} to be syndicated to {}",
          revisionId, syndicationTargetQueue);
      return syndication;
    } catch (Exception e) {
      log.warn("Error syndicating " + revisionId + " to " + syndicationTargetQueue, e);
      return updateSyndication(revisionId, syndicationTargetQueue,
          SyndicationStatus.FAILURE.getLabel(), e.getMessage());
    }
  }

  private String createBody(ArticleRevisionIdentifier revisionId) {
    StringBuilder body = new StringBuilder();
    body.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        .append("<ambraMessage>")
        .append("<doi>").append(revisionId.getDoiName()).append("</doi>")
        .append("<revision>").append(revisionId.getRevision()).append("</revision>")
        .append("</ambraMessage>");

    return body.toString();
  }
}
