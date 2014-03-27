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
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import org.ambraproject.filestore.FSIDMapper;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.ambraproject.models.Journal;
import org.ambraproject.models.Syndication;
import org.ambraproject.queue.MessageSender;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleStateService;
import org.ambraproject.rhino.service.IngestibleService;
import org.ambraproject.rhino.view.article.ArticleInputView;
import org.ambraproject.service.article.NoSuchArticleIdException;
import org.ambraproject.util.DocumentBuilderFactoryCreator;
import org.apache.commons.configuration.Configuration;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * {@inheritDoc}
 */
public class ArticleStateServiceImpl extends AmbraService implements ArticleStateService {

  private static final String XML_NAMESPACE = "http://www.ambraproject.org/article/additionalInfo";

  @Autowired
  private ArticleCrudService articleCrudService;

  @Autowired
  @VisibleForTesting
  public MessageSender messageSender;

  @Autowired
  private Configuration ambraConfiguration;

  @Autowired
  private IngestibleService ingestibleService;

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
   * @throws FileStoreException
   * @throws IOException
   */
  private void updateSolrIndex(ArticleIdentity articleId, Article article, boolean isPublished)
      throws FileStoreException, IOException {
    if (isPublished) {
      Document doc;
      try (InputStream xml = articleCrudService.readXml(articleId)) {

        // TODO: is it necessary and/or performant to create a new one of these each
        // time?  The old admin codebase a DocumentBuilderFactory as an instance field
        // and synchronizes access to it.
        DocumentBuilderFactory documentBuilderFactory
            = DocumentBuilderFactoryCreator.createFactory();
        DocumentBuilder documentBuilder;
        try {
          documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
          throw new RuntimeException(pce);
        }
        try {
          doc = documentBuilder.parse(xml);
        } catch (SAXException se) {
          throw new RuntimeException("Bad XML retrieved from filestore for " + article.getDoi(), se);
        }
      }
      doc = appendJournals(doc, articleId);
      doc = appendStrikingImage(doc, article);
      messageSender.sendMessage(ambraConfiguration.getString(
          "ambra.services.search.articleIndexingQueue", null), doc);
    } else {
      messageSender.sendMessage(ambraConfiguration.getString("ambra.services.search.articleDeleteQueue", null),
          articleId.getKey());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Article update(ArticleIdentity articleId, ArticleInputView input)
      throws FileStoreException, IOException {
    Article article = loadArticle(articleId);

    Optional<Integer> updatedState = input.getPublicationState();
    if (updatedState.isPresent()) {
      if (updatedState.get() == Article.STATE_ACTIVE && article.getState() == Article.STATE_DISABLED) {
        throw new RestClientException("A disabled article cannot be published; it must first be re-ingested",
            HttpStatus.METHOD_NOT_ALLOWED);
      } else {
        article.setState(updatedState.get());
      }

      boolean isPublished = (article.getState() == Article.STATE_ACTIVE);
      updateSolrIndex(articleId, article, isPublished);
      hibernateTemplate.update(article);

      if (updatedState.get() == Article.STATE_DISABLED) {
        deleteFilestoreFiles(articleId);
        ingestibleService.revertArchive(articleId);
      }
    }

    for (ArticleInputView.SyndicationUpdate update : input.getSyndicationUpdates()) {

      // TODO: should we always re-attempt the syndication, as we do here, if it's
      // IN_PROGRESS?  Or base it on the Syndication.status of the appropriate target?
      // Not sure yet.
      if (update.getStatus().equals(Syndication.STATUS_IN_PROGRESS)) {
        try {
          syndicationService.syndicate(article.getDoi(), update.getTarget());
        } catch (NoSuchArticleIdException nsaide) {

          // Should never happen since we just loaded the article.
          throw new RuntimeException(nsaide);
        }
      }

      // TODO: un-syndicate, if necessary.
    }

    return article;
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

  /**
   * Deletes all the files associated with an article from the filestore.
   *
   * @param articleId identifies the article
   * @throws FileStoreException
   * @throws IOException
   */
  private void deleteFilestoreFiles(ArticleIdentity articleId) throws FileStoreException, IOException {
    String articleRoot = FSIDMapper.zipToFSID(articleId.getKey(), "");
    Map<String, String> files = fileStoreService.listFiles(articleRoot);

    for (String file : files.keySet()) {
      String fullFile = FSIDMapper.zipToFSID(articleId.getKey(), file);
      fileStoreService.deleteFile(fullFile);
    }
  }
}
