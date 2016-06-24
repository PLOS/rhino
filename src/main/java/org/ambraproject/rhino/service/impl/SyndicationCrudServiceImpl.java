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
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.service.MessageSender;
import org.ambraproject.rhino.service.SyndicationCrudService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.article.SyndicationOutputView;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationKey;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.hibernate.Query;
import org.omg.CORBA.portable.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
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
  public Syndication getSyndication(final ArticleVersionIdentifier versionIdentifier, final String syndicationTarget) {
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "FROM Syndication s " +
          "WHERE s.target = :target " +
          "AND s.articleVersion.article.doi = :doi " +
          "AND s.articleVersion.revisionNumber = :revisionNumber");
      query.setParameter("target", syndicationTarget);
      query.setParameter("doi", versionIdentifier.getDoiName());
      query.setParameter("revisionNumber", versionIdentifier.getRevision());
      return (Syndication) query.uniqueResult();
    });
  }

  @Transactional(readOnly = true)
  @SuppressWarnings("unchecked")
  public List<Syndication> getSyndications(final ArticleVersionIdentifier versionIdentifier) {
    return hibernateTemplate.execute(session -> {
      Query query = session.createQuery("" +
          "FROM Syndication s " +
          "WHERE s.articleVersion.article.doi = :doi " +
          "AND s.articleVersion.revisionNumber = :revisionNumber");
      query.setParameter("doi", versionIdentifier.getDoiName());
      query.setParameter("revisionNumber", versionIdentifier.getRevision());
      return (List<Syndication>) query.list();
    });
  }

  @Transactional(rollbackFor = {Throwable.class})
  @Override
  public Syndication updateSyndication(final ArticleVersionIdentifier articleVersionIdentifier,
      final String syndicationTarget, final String status, final String errorMessage) {
    Syndication syndication = getSyndication(articleVersionIdentifier, syndicationTarget);
    if (syndication == null) {
      throw new RuntimeException("No such syndication for doi " + articleVersionIdentifier
          + " and target " + syndicationTarget);
    }
    syndication.setStatus(status);
    syndication.setErrorMessage(errorMessage);
    hibernateTemplate.update(syndication);

    return syndication;
  }

  @Transactional(rollbackFor = {Throwable.class})
  @Override
  @SuppressWarnings("unchecked")
  public List<Syndication> createSyndications(ArticleVersionIdentifier versionIdentifier) {

    List<HierarchicalConfiguration> allSyndicationTargets = ((HierarchicalConfiguration)
        configuration).configurationsAt("ambra.services.syndications.syndication");

    if (allSyndicationTargets == null || allSyndicationTargets.size() < 1) { // Should never happen.
      log.warn("There are no Syndication Targets defined in the property: " +
          "ambra.services.syndications.syndication so no Syndication objects were created for " +
          "the article with ID = " + versionIdentifier);
      return new ArrayList<>();
    }

    List<Syndication> syndications = new ArrayList<>(allSyndicationTargets.size());

    for (HierarchicalConfiguration targetNode : allSyndicationTargets) {
      String target = targetNode.getString("[@target]");
      Syndication existingSyndication = getSyndication(versionIdentifier, target);
      //todo: cleanup - this list return is not used
      if (existingSyndication != null) {
        syndications.add(existingSyndication);
      } else {
        syndications.add(createSyndication(versionIdentifier, target));
      }
    }
    return syndications;
  }

  @Override
  public Syndication createSyndication(ArticleVersionIdentifier versionIdentifier, String target) {
    ArticleVersion articleVersion = articleCrudService.getArticleVersion(versionIdentifier);
    Syndication syndication = new Syndication(articleVersion, target);
    syndication.setStatus(Syndication.STATUS_PENDING);
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
    return new Transceiver() {
      @Override
      protected Object getData() throws IOException {
        List<Syndication> syndications = getSyndications(journalKey, statuses);
        return syndications.stream()
            .map(new SyndicationOutputView.Factory()::createSyndicationView)
            .collect(Collectors.toList());
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  @Transactional(rollbackFor = {Throwable.class})
  @Override
  public Syndication syndicate(ArticleVersionIdentifier articleVersionIdentifier, String syndicationTarget) {
    ArticleVersion articleVersion = articleCrudService.getArticleVersion(articleVersionIdentifier);

    Syndication syndication = getSyndication(articleVersionIdentifier, syndicationTarget);
    if (syndication == null) {
      //no existing syndication
      syndication = new Syndication(articleVersion, syndicationTarget);
      syndication.setStatus(Syndication.STATUS_IN_PROGRESS);
      syndication.setErrorMessage(null);
      syndication.setSubmissionCount(1);
      syndication.setLastSubmitTimestamp(new Date());
      hibernateTemplate.save(syndication);
    } else {
      syndication.setStatus(Syndication.STATUS_IN_PROGRESS);
      syndication.setErrorMessage(null);
      syndication.setSubmissionCount(syndication.getSubmissionCount() + 1);
      syndication.setLastSubmitTimestamp(new Date());
      hibernateTemplate.update(syndication);
    }

    try {
      //Send message
      sendSyndicationMessage(syndicationTarget, articleVersionIdentifier);
      log.info("Successfully sent a Message to plos-queue for {} to be syndicated to {}",
          articleVersionIdentifier, syndicationTarget);
      return syndication;
    } catch (Exception e) {
      log.warn("Error syndicating " + articleVersionIdentifier + " to " + syndicationTarget, e);
      //update to failure and return updated syndication
      return updateSyndication(articleVersionIdentifier, syndicationTarget, Syndication.STATUS_FAILURE, e.getMessage());
    }
  }

  private void sendSyndicationMessage(String target, ArticleVersionIdentifier articleVersionId)
      throws ApplicationException {
    List<HierarchicalConfiguration> syndications = ((HierarchicalConfiguration) configuration)
        .configurationsAt("ambra.services.syndications.syndication");

    String queue = null;
    String additionalBodyContent = null;
    if (syndications != null) {
      for (HierarchicalConfiguration syndication : syndications) {
        if (target.equals(syndication.getString("[@target]"))) {
          queue = syndication.getString("queue", null);
          HierarchicalConfiguration message = (HierarchicalConfiguration) syndication.subset("message");
          additionalBodyContent = createAdditionalBodyFromConfiguration(message);
        }
      }
    }

    if (queue == null) {
      throw new RuntimeException(target + " queue not configured");
    }

    messageSender.sendBody(queue, createBody(articleVersionId, additionalBodyContent));

  }

  /**
   * If there is content inside <syndication><message> configuration append it to message.
   *
   * @param configuration Sub configuration under "message" tag.
   * @return XML code snippet.
   */
  private static String createAdditionalBodyFromConfiguration(HierarchicalConfiguration configuration) {
    if (configuration == null || configuration.isEmpty()) {
      return null;
    }
    Visitor visitor = new Visitor();
    configuration.getRoot().visit(visitor, new ConfigurationKey(""));
    return visitor.body.toString();
  }

  /**
   * Visitor class for rendering XML string from configuration. Warning: will not handle XML attributes in
   * configuration.
   */
  private static class Visitor extends HierarchicalConfiguration.NodeVisitor {
    private final StringBuilder body = new StringBuilder();

    @Override
    public void visitBeforeChildren(HierarchicalConfiguration.Node node, ConfigurationKey configurationKey) {
      String name = node.getName();
      if (name != null) {
        body.append('<').append(name).append('>');
      }
      Object value = node.getValue();
      if (value != null) {
        body.append((String) value);
      }
      super.visitBeforeChildren(node, configurationKey);
    }

    @Override
    public void visitAfterChildren(HierarchicalConfiguration.Node node, ConfigurationKey configurationKey) {
      String name = node.getName();
      if (name != null) {
        body.append("</").append(name).append('>');
      }
      super.visitAfterChildren(node, configurationKey);
    }
  }

  private String createBody(ArticleVersionIdentifier articleVersionId, String additionalBodyContent) {
    StringBuilder body = new StringBuilder();
    body.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        .append("<ambraMessage>")
        .append("<doi>").append(articleVersionId.getDoiName()).append("</doi>")
        .append("<version>").append(articleVersionId.getRevision()).append("</doi>");

    if (additionalBodyContent != null) {
      body.append(additionalBodyContent);
    }

    body.append("</ambraMessage>");

    return body.toString();
  }
}
