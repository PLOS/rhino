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

import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Syndication;
import org.ambraproject.rhino.service.JournalReadService;
import org.ambraproject.rhino.service.MessageSender;
import org.ambraproject.rhino.service.SyndicationService;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationKey;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.omg.CORBA.portable.ApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Manage the syndication process, including creating and updating Syndication objects, as well as pushing syndication
 * messages to a message queue.
 *
 * @author Scott Sterling
 * @author Joe Osowski
 */
public class SyndicationServiceImpl extends AmbraService implements SyndicationService {
  private static final Logger log = LoggerFactory.getLogger(SyndicationServiceImpl.class);

  @Autowired
  private Configuration configuration;

  @Autowired
  private MessageSender messageSender;

  @Autowired
  private JournalReadService journalService;

  private final DateFormat mysqlDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");


  /**
   * @param articleTypes All the article types of the Article for which Syndication objects are being created
   * @return Whether to create a Syndication object for this Article
   */
  private boolean isSyndicatableType(Set<String> articleTypes) {
    String articleTypeDoNotCreateSyndication = "http://rdf.plos.org/RDF/articleType/Issue%20Image";
    return !(articleTypes != null && articleTypes.contains(articleTypeDoNotCreateSyndication));
  }

  @Override
  @SuppressWarnings("unchecked")
  public Syndication getSyndication(final String articleDoi, final String syndicationTarget) {
    return (Syndication) DataAccessUtils.uniqueResult(hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Syndication.class)
            .add(Restrictions.eq("target", syndicationTarget))
            .add(Restrictions.eq("doi", articleDoi))));
  }

  @Transactional(rollbackFor = {Throwable.class})
  @Override
  public Syndication updateSyndication(final String articleDoi, final String syndicationTarget, final String status,
                                       final String errorMessage) {
    Syndication syndication = getSyndication(articleDoi, syndicationTarget);
    if (syndication == null) {
      throw new RuntimeException("No such syndication for doi " + articleDoi + " and target " + syndicationTarget);
    }
    syndication.setStatus(status);
    syndication.setErrorMessage(errorMessage);
    hibernateTemplate.update(syndication);

    return syndication;
  }

  @Transactional(rollbackFor = {Throwable.class})
  @Override
  @SuppressWarnings("unchecked")
  public List<Syndication> createSyndications(String articleDoi) {
    Article article = (Article) DataAccessUtils.uniqueResult(hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Article.class)
            .add(Restrictions.eq("doi", articleDoi))));
    if (article == null) {
      throw new NoSuchArticleIdException(articleDoi);
    }
    if (!isSyndicatableType(article.getTypes())) {
      //don't syndicate
      return new ArrayList<>();
    }
    List<HierarchicalConfiguration> allSyndicationTargets = ((HierarchicalConfiguration)
        configuration).configurationsAt("ambra.services.syndications.syndication");

    if (allSyndicationTargets == null || allSyndicationTargets.size() < 1) { // Should never happen.
      log.warn("There are no Syndication Targets defined in the property: " +
          "ambra.services.syndications.syndication so no Syndication objects were created for " +
          "the article with ID = " + articleDoi);
      return new ArrayList<>();
    }

    List<Syndication> syndications = new ArrayList<Syndication>(allSyndicationTargets.size());

    for (HierarchicalConfiguration targetNode : allSyndicationTargets) {
      String target = targetNode.getString("[@target]");
      Syndication existingSyndication = getSyndication(articleDoi, target);
      if (existingSyndication != null) {
        syndications.add(existingSyndication);
      } else {
        Syndication syndication = new Syndication(articleDoi, target);
        syndication.setStatus(Syndication.STATUS_PENDING);
        syndication.setSubmissionCount(0);
        hibernateTemplate.save(syndication);
        syndications.add(syndication);
      }
    }
    return syndications;
  }


  @Transactional
  @SuppressWarnings("unchecked")
  @Override
  public List<Syndication> getFailedAndInProgressSyndications(final String journalKey) {
    Integer numDaysInPast = configuration.getInteger(
        "ambra.virtualJournals." + journalKey + ".syndications.display.numDaysInPast", 30);

    // The most recent midnight.  No need to futz about with exact dates.
    final Calendar start = Calendar.getInstance();
    start.set(Calendar.HOUR, 0);
    start.set(Calendar.MINUTE, 0);
    start.set(Calendar.SECOND, 0);
    start.set(Calendar.MILLISECOND, 0);

    final Calendar end = (Calendar) start.clone(); // The most recent midnight (last night)

    start.add(Calendar.DATE, -(numDaysInPast));
    end.add(Calendar.DATE, 1); // Include everything that happened today.

    final Journal journal = journalService.getJournal(journalKey);

    if (journal == null) {
      throw new RuntimeException("Could not find journal for journal key: " + journalKey);
    }

    return (List<Syndication>) hibernateTemplate.execute(new HibernateCallback() {
      public Object doInHibernate(Session session) throws HibernateException, SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("select {s.*} from syndication s ")
            .append("join article a on s.doi = a.doi ")
            .append("where s.status in ('" + Syndication.STATUS_IN_PROGRESS + "','" + Syndication.STATUS_FAILURE + "')")
            .append("and s.lastModified between '").append(mysqlDateFormat.format(start.getTime())).append("'")
            .append(" and '").append(mysqlDateFormat.format(end.getTime())).append("' and ")
            .append("a.eIssn = '").append(journal.geteIssn()).append("'");

        return session.createSQLQuery(sql.toString())
            .addEntity("s", Syndication.class).list();
      }
    });
  }

  @Transactional(readOnly = true)
  @SuppressWarnings("unchecked")
  public List<Syndication> getSyndications(final String articleDoi) {
    return (List<Syndication>) hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Syndication.class)
            .add(Restrictions.eq("doi", articleDoi)));
  }

  @Transactional(rollbackFor = {Throwable.class})
  @SuppressWarnings("unchecked")
  @Override
  public Syndication syndicate(String articleDoi, String syndicationTarget) {
    List<Article> matchingArticle = (List<Article>) hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Article.class)
            .add(Restrictions.eq("doi", articleDoi)), 0, 1);
    if (matchingArticle.size() == 0) {
      throw new NoSuchArticleIdException("The article may have been deleted."
          + " Please reingest this article before attempting to syndicate to it.");
    }
    final String archiveName = matchingArticle.get(0).getArchiveName();
    if (archiveName == null || archiveName.isEmpty()) {
      throw new RuntimeException("The article " + articleDoi
          + " does not have an archive file associated with it.");
    }

    Syndication syndication = getSyndication(articleDoi, syndicationTarget);
    if (syndication == null) {
      //no existing syndication
      syndication = new Syndication(articleDoi, syndicationTarget);
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
      //  Send message.
      sendSyndicationMessage(syndicationTarget, articleDoi, archiveName);
      log.info("Successfully sent a Message to plos-queue for {} to be syndicated to {}", articleDoi, syndicationTarget);
      return syndication;
    } catch (Exception e) {
      log.warn("Error syndicating " + articleDoi + " to " + syndicationTarget, e);
      //update to failure and return updated syndication
      return updateSyndication(articleDoi, syndicationTarget, Syndication.STATUS_FAILURE, e.getMessage());
    }
  }

  private void sendSyndicationMessage(String target, String articleId, String archive)
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

    messageSender.sendBody(queue, createBody(articleId, archive, additionalBodyContent));

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

  private String createBody(String articleId, String archive, String additionalBodyContent) {
    StringBuilder body = new StringBuilder();
    body.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        .append("<ambraMessage>")
        .append("<doi>").append(articleId).append("</doi>")
        .append("<archive>").append(archive).append("</archive>");

    if (additionalBodyContent != null) {
      body.append(additionalBodyContent);
    }

    body.append("</ambraMessage>");

    return body.toString();
  }

  private class NoSuchArticleIdException extends RuntimeException {
    private NoSuchArticleIdException(String articleDoi) {
      super("No such article: " + articleDoi);
    }
  }

}