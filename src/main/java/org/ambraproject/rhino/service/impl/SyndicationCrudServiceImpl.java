/*
 * $HeadURL$
 * $Id$
 *
 * Copyright (c) 2006-2010 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.service.impl;

import org.ambraproject.rhino.identity.ArticleVersionIdentifier;
import org.ambraproject.rhino.model.ArticleVersion;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Syndication;
import org.ambraproject.rhino.model.SyndicationStatus;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.service.MessageSender;
import org.ambraproject.rhino.service.SyndicationCrudService;
import org.ambraproject.rhino.util.response.EntityCollectionTransceiver;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.article.ArticleJsonNames;
import org.ambraproject.rhino.view.article.SyndicationOutputView;
import org.apache.commons.configuration.Configuration;
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
import java.util.Calendar;
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
  private Configuration configuration;

  @Autowired
  private MessageSender messageSender;

  @Autowired
  private JournalCrudService journalService;

  @Autowired
  private ArticleCrudService articleCrudService;

  @Override
  @SuppressWarnings("unchecked")
  public Syndication getSyndication(final ArticleVersionIdentifier versionId,
      final String syndicationTargetQueue) {
    ArticleVersion articleVersion = articleCrudService.getArticleVersion(versionId);
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "FROM Syndication s " +
          "WHERE s.targetQueue = :targetQueue " +
          "AND s.articleVersion = :articleVersion");
      query.setParameter(ArticleJsonNames.SYNDICATION_TARGET, syndicationTargetQueue);
      query.setParameter("articleVersion", articleVersion);
      return (Syndication) query.uniqueResult();
    });
  }

  @Transactional(readOnly = true)
  @SuppressWarnings("unchecked")
  public List<Syndication> getSyndications(final ArticleVersionIdentifier versionId) {
    ArticleVersion articleVersion = articleCrudService.getArticleVersion(versionId);
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "FROM Syndication s " +
          "WHERE s.articleVersion = :articleVersion");
      query.setParameter("articleVersion", articleVersion);
      return (List<Syndication>) query.list();
    });
  }

  @Transactional(rollbackFor = {Throwable.class})
  @Override
  public Syndication updateSyndication(final ArticleVersionIdentifier versionId,
      final String syndicationTargetQueue, final String status, final String errorMessage) {
    Syndication syndication = getSyndication(versionId, syndicationTargetQueue);
    if (syndication == null) {
      throw new RuntimeException("No such syndication for doi " + versionId
          + " and target " + syndicationTargetQueue);
    }
    syndication.setStatus(status);
    syndication.setErrorMessage(errorMessage);
    hibernateTemplate.update(syndication);

    return syndication;
  }

  @Override
  public Syndication createSyndication(ArticleVersionIdentifier versionId, String syndicationTargetQueue) {
    ArticleVersion articleVersion = articleCrudService.getArticleVersion(versionId);
    Syndication syndication = new Syndication(articleVersion, syndicationTargetQueue);
    syndication.setStatus(SyndicationStatus.PENDING.getLabel());
    syndication.setSubmissionCount(0);
    hibernateTemplate.save(syndication);
    return syndication;
  }

  @Transactional
  @Override
  public List<Syndication> getSyndications(final String journalKey, final List<String> statuses) {
    Integer numDaysInPast = configuration.getInteger(
        "ambra.virtualJournals." + journalKey + ".syndications.display.numDaysInPast", 30);

    LocalDate startDate = LocalDate.now().minus(numDaysInPast, ChronoUnit.DAYS);
    Instant startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

    final Journal journal = journalService.findJournal(journalKey);

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
      return (List<Syndication>) query.list();
    });
  }

  @Override
  public Transceiver readSyndications(String journalKey, List<String> statuses) throws IOException {
    return new EntityCollectionTransceiver<Syndication>() {

      @Override
      protected Object getView(Collection<? extends Syndication> syndications) {
        return syndications.stream()
            .map(SyndicationOutputView::createSyndicationView)
            .collect(Collectors.toList());
      }

      @Override
      protected Collection<? extends Syndication> fetchEntities() {
        return getSyndications(journalKey, statuses);
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  @Transactional(rollbackFor = {Throwable.class})
  @Override
  public Syndication syndicate(ArticleVersionIdentifier versionId, String syndicationTargetQueue) {
    ArticleVersion articleVersion = articleCrudService.getArticleVersion(versionId);

    Syndication syndication = getSyndication(versionId, syndicationTargetQueue);
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
      messageSender.sendBody(syndicationTargetQueue, createBody(versionId));
      log.info("Successfully sent a Message to plos-queue for {} to be syndicated to {}",
          versionId, syndicationTargetQueue);
      return syndication;
    } catch (Exception e) {
      log.warn("Error syndicating " + versionId + " to " + syndicationTargetQueue, e);
      return updateSyndication(versionId, syndicationTargetQueue,
          SyndicationStatus.FAILURE.getLabel(), e.getMessage());
    }
  }

  private String createBody(ArticleVersionIdentifier versionId) {
    StringBuilder body = new StringBuilder();
    body.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        .append("<ambraMessage>")
        .append("<doi>").append(versionId.getDoiName()).append("</doi>")
        .append("<version>").append(versionId.getRevision()).append("</doi>")
        .append("</ambraMessage>");

    return body.toString();
  }
}
