/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2013 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.service.impl;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleAsset;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.PublicationState;
import org.ambraproject.rhino.model.SyndicationStatus;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleStateService;
import org.ambraproject.rhino.service.IngestibleService;
import org.ambraproject.rhino.service.MessageSender;
import org.ambraproject.rhino.service.SyndicationCrudService;
import org.ambraproject.rhino.view.article.ArticleInputView;
import org.apache.camel.CamelExecutionException;
import org.apache.commons.configuration.Configuration;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@inheritDoc}
 */
public class ArticleStateServiceImpl extends AmbraService implements ArticleStateService {

  private static final Logger log = LoggerFactory.getLogger(ArticleStateServiceImpl.class);

  private static final String XML_NAMESPACE = "http://www.ambraproject.org/article/additionalInfo";

  /**
   * The key to fetch the value for the authorization ID for the given request in the header
   */
  @VisibleForTesting
  public static final String HEADER_AUTH_ID = "authId";

  @Autowired
  private ArticleCrudService articleCrudService;

  @Autowired
  @VisibleForTesting
  public MessageSender messageSender;

  @Autowired
  private Configuration ambraConfiguration;

  @Autowired
  private IngestibleService ingestibleService;

  @Autowired
  private SyndicationCrudService syndicationService;

  /**
   * Attaches additional XML info to an article document specifying the journals it is published in.
   * <p/>
   * This is largely copied from org.ambraproject.article.service.ArticleDocumentServiceImpl in the old admin codebase.
   *
   * @param doc       article XML
   * @param articleId encodes DOI
   * @return doc
   */
  private Document appendJournals(Document doc, ArticleIdentity articleId) {
    Article article = (Article) DataAccessUtils.uniqueResult(
        (List<?>) hibernateTemplate.findByCriteria(
            DetachedCriteria.forClass(Article.class)
                .setFetchMode("journals", FetchMode.JOIN)
                .add(Restrictions.eq("doi", articleId.getKey()))
        )
    );
    Set<Journal> journals = article.getJournals();

    Element additionalInfoElement = doc.createElementNS(XML_NAMESPACE, "ambra");
    Element journalsElement = doc.createElementNS(XML_NAMESPACE, "journals");
    doc.getDocumentElement().appendChild(additionalInfoElement);
    additionalInfoElement.appendChild(journalsElement);

    for (Journal journal : journals) {
      Element journalElement = doc.createElementNS(XML_NAMESPACE, "journal");
      Element eIssn = doc.createElementNS(XML_NAMESPACE, "eIssn");
      eIssn.appendChild(doc.createTextNode(journal.geteIssn()));
      journalElement.appendChild(eIssn);
      Element key = doc.createElementNS(XML_NAMESPACE, "key");
      key.appendChild(doc.createTextNode(journal.getJournalKey()));
      journalElement.appendChild(key);
      Element name = doc.createElementNS(XML_NAMESPACE, "name");
      name.appendChild(doc.createTextNode(journal.getTitle()));
      journalElement.appendChild(name);
      journalsElement.appendChild(journalElement);
    }
    return doc;
  }

  /**
   * Append an additional XML node to an article document specifying the striking image.
   * <p/>
   * This is largely copied from org.ambraproject.search.service.IndexingServiceImpl in the old admin codebase.
   *
   * @param doc     article XML
   * @param article
   * @return doc
   */
  private Document appendStrikingImage(Document doc, Article article) {
    String strikingImage = Strings.nullToEmpty(article.getStrkImgURI());
    NodeList metaNodeLst = doc.getElementsByTagName("article-meta");
    Node metaNode = metaNodeLst.item(0);
    Element strkImgElem = doc.createElement("article-strkImg");

    strkImgElem.setTextContent(strikingImage);
    metaNode.appendChild(strkImgElem.cloneNode(true));
    return doc;
  }

  /**
   * Updates the solr index (indirectly, via plos-queue) depending on the publication state of an article.
   *
   * @param articleId   wraps the DOI
   * @param article
   * @param isPublished indicates whether we are publishing or un-publishing
   * @throws IOException
   */
  private void updateSolrIndex(ArticleIdentity articleId, Article article, boolean isPublished)
      throws IOException {
    if (isPublished) {
      Document doc;
      try (InputStream xml = articleCrudService.readXml(articleId)) {

        // TODO: is it necessary and/or performant to create a new one of these each
        // time?  The old admin codebase a DocumentBuilderFactory as an instance field
        // and synchronizes access to it.
        DocumentBuilder documentBuilder = newDocumentBuilder();
        try {
          doc = documentBuilder.parse(xml);
        } catch (SAXException se) {
          throw new RuntimeException("Bad XML retrieved for " + article.getDoi(), se);
        }
      }
      doc = appendJournals(doc, articleId);
      doc = appendStrikingImage(doc, article);
      messageSender.sendBody(ambraConfiguration.getString(
          "ambra.services.search.articleIndexingQueue", null), doc);
    } else {
      messageSender.sendBody(ambraConfiguration.getString("ambra.services.search.articleDeleteQueue", null),
          articleId.getKey());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Article update(ArticleIdentity articleId, ArticleInputView input)
      throws IOException {
    Article article = loadArticle(articleId);

    Optional<PublicationState> updatedState = input.getPublicationState().map(PublicationState::fromValue);
    if (updatedState.isPresent()) {
      PublicationState articleState = PublicationState.fromValue(article.getState());
      if (updatedState.get() == PublicationState.PUBLISHED && articleState == PublicationState.DISABLED) {
        throw new RestClientException("A disabled article cannot be published; it must first be re-ingested",
            HttpStatus.METHOD_NOT_ALLOWED);
      } else {
        article.setState(updatedState.get().getValue());
      }

      boolean isPublished = (articleState == PublicationState.PUBLISHED);
      updateSolrIndex(articleId, article, isPublished);
      hibernateTemplate.update(article);

      if (updatedState.get() == PublicationState.PUBLISHED) {
        queueCrossRefRefresh(article.getDoi());
      }

      if (updatedState.get() == PublicationState.DISABLED) {
        for (ArticleAsset asset : article.getAssets()) {
          deleteAssetFile(AssetFileIdentity.from(asset));
        }
        ingestibleService.revertArchive(articleId);
      }
    }

    for (ArticleInputView.SyndicationUpdate update : input.getSyndicationUpdates()) {

      // TODO: should we always re-attempt the syndication, as we do here, if it's
      // IN_PROGRESS?  Or base it on the Syndication.status of the appropriate target?
      // Not sure yet.
      if (update.getStatus().equals(SyndicationStatus.IN_PROGRESS.getLabel())) {
        //syndicationService.syndicate(article.getDoi(), update.getTarget()); todo: implement with versioning
      }

      // TODO: un-syndicate, if necessary.
    }

    return article;
  }

  /**
   * Send a message to the queue to refresh the cited articles via cross ref
   */
  @SuppressWarnings("unchecked")
  private void queueCrossRefRefresh(String doi) {
    String refreshCitedArticlesQueue = ambraConfiguration.getString("ambra.services.queue.refreshCitedArticles", null);
    if (refreshCitedArticlesQueue != null) {
      try {
        messageSender.sendBodyAndHeaders(refreshCitedArticlesQueue, doi, new HashMap() {{
          //Appending null for the header auth ID.  It's OK because the article must be in a published
          //state and the authID should not be checked
          put(HEADER_AUTH_ID, null);
        }});
      } catch (CamelExecutionException ex) {
        log.error(ex.getMessage(), ex);
        throw new RuntimeException("Failed to queue job for refreshing article references, is the queue running?");
      }
    } else {
      throw new RuntimeException("Refresh cited articles queue not defined. No route created.");
    }
  }

  private Article loadArticle(ArticleIdentity articleId) {
    Article result = (Article) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Article.class)
            .add(Restrictions.eq("doi", articleId.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    if (result == null) {
      throw new RestClientException("Article not found: " + articleId.getIdentifier(),
          HttpStatus.NOT_FOUND);
    }
    return result;
  }

}
