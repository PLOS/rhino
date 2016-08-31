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
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleTable;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.SolrIndexService;
import org.ambraproject.rhino.service.MessageSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * {@inheritDoc}
 */
public class SolrIndexServiceImpl extends AmbraService implements SolrIndexService {

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
  private RuntimeConfiguration runtimeConfiguration;

  /**
   * Attaches additional XML info to an article document specifying the journals it is published in.
   * <p/>
   * This is largely copied from org.ambraproject.article.service.ArticleDocumentServiceImpl in the old admin codebase.
   *
   * @param doc       article XML
   * @param ingestion encodes DOI
   * @return doc
   */
  private Document appendJournals(Document doc, ArticleIngestion ingestion) {
    Element additionalInfoElement = doc.createElementNS(XML_NAMESPACE, "ambra");
    Element journalsElement = doc.createElementNS(XML_NAMESPACE, "journals");
    doc.getDocumentElement().appendChild(additionalInfoElement);
    additionalInfoElement.appendChild(journalsElement);

    Journal journal = ingestion.getJournal();
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

    return doc;
  }

  /**
   * Append an additional XML node to an article document specifying the striking image.
   *
   * @param doc       article XML
   * @param ingestion
   * @return doc
   */
  private Document appendStrikingImage(Document doc, ArticleIngestion ingestion) {
    String strikingImage = null; // TODO: Recover from ingestion object when it is supported
    NodeList metaNodeLst = doc.getElementsByTagName("article-meta");
    Node metaNode = metaNodeLst.item(0);
    Element strkImgElem = doc.createElement("article-strkImg");

    strkImgElem.setTextContent(Strings.nullToEmpty(strikingImage));
    metaNode.appendChild(strkImgElem.cloneNode(true));
    return doc;
  }

  @Override
  public void updateSolrIndex(ArticleIdentifier articleId) {
    ArticleTable article = articleCrudService.readArticle(articleId);
    ArticleIngestion ingestion = articleCrudService.readLatestRevision(article).getIngestion();
    Document doc = articleCrudService.getManuscriptXml(ingestion);

    doc = appendJournals(doc, ingestion);
    doc = appendStrikingImage(doc, ingestion);

    String destination = runtimeConfiguration.getQueueConfiguration().getSolrUpdate();
    if (destination == null) {
      throw new RuntimeException("solrUpdate is not configured");
    }
    messageSender.sendBody(destination, doc);
  }

  @Override
  public void removeSolrIndex(ArticleIdentifier articleId) {
    String doi = articleId.getDoi().asUri(Doi.UriStyle.INFO_DOI).toString();
    String destination = runtimeConfiguration.getQueueConfiguration().getSolrDelete();
    if (destination == null) {
      throw new RuntimeException("solrDelete is not configured");
    }
    messageSender.sendBody(destination, doi);
  }

}
