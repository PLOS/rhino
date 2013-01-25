/*
 * Copyright (c) 2006-2012 by Public Library of Science
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

package org.ambraproject.rhino.content.xml;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAuthor;
import org.ambraproject.models.ArticleEditor;
import org.ambraproject.models.CitedArticle;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.util.NodeListAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * An NLM-format XML document that can be "ingested" to build an {@link Article} object.
 */
public class ArticleXml extends AbstractArticleXml<Article> {

  private static final Logger log = LoggerFactory.getLogger(ArticleXml.class);

  /*
   * As specified by <http://dtd.nlm.nih.gov/publishing/2.0/journalpublishing.dtd> (see <!ENTITY % article-types>)
   */
  private static final ImmutableSet<String> VALID_ARTICLE_TYPES = ImmutableSet.copyOf(new String[]{
      "article-commentary", "abstract", "addendum", "announcement", "book-review", "books-received", "brief-report",
      "calendar", "case-report", "correction", "discussion", "editorial", "in-brief", "introduction", "letter",
      "meeting-report", "news", "obituary", "oration", "other", "product-review", "research-article", "retraction",
      "reply", "review-article",
  });

  private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

  public ArticleXml(Document xml) {
    super(xml);
  }


  /**
   * @return
   * @throws XmlContentException if the DOI is not present
   */
  public ArticleIdentity readDoi() throws XmlContentException {
    String doi = readString("/article/front/article-meta/article-id[@pub-id-type=\"doi\"]");
    if (doi == null) {
      throw new XmlContentException("DOI not found");
    }
    return ArticleIdentity.create(doi);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<AssetNode> findAllAssetNodes() {
    return super.findAllAssetNodes();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Article build(Article article) throws XmlContentException {
    setConstants(article);
    setFromXml(article);
    return article;
  }

  /**
   * Set constant values.
   *
   * @param article the article to modify
   */
  private static void setConstants(Article article) {
    // These are constants because they are implied by how we get the input
    article.setFormat("text/xml");
    article.setState(Article.STATE_UNPUBLISHED);
  }

  private void setFromXml(Article article) throws XmlContentException {
    article.setTitle(readString("/article/front/article-meta/title-group/article-title"));
    article.seteIssn(readString("/article/front/journal-meta/issn[@pub-type=\"epub\"]"));
    article.setDescription(buildDescription(readNode("/article/front/article-meta/abstract")));
    article.setRights(buildRights());
    article.setPages(buildPages());
    article.seteLocationId(readString("/article/front/article-meta/elocation-id"));
    article.setVolume(readString("/article/front/article-meta/volume"));
    article.setIssue(readString("/article/front/article-meta/issue"));
    String journal = buildJournal();
    if (journal != null && !journal.isEmpty()) {
      article.setJournal(journal);
    }
    article.setPublisherName(readString("/article/front/journal-meta/publisher/publisher-name"));
    article.setPublisherLocation(readString("/article/front/journal-meta/publisher/publisher-loc"));

    article.setLanguage(parseLanguage(readString("/article/@xml:lang")));
    article.setDate(parseDate(readNode("/article/front/article-meta/pub-date[@pub-type=\"epub\"]")));
    article.setTypes(parseArticleTypes(readTextList("/article/@article-type")));
    article.setCitedArticles(parseCitations(readNodeList("/article/back/ref-list//(citation|nlm-citation)")));
    article.setAuthors(readAuthors(readNodeList(
        "/article/front/article-meta/contrib-group/contrib[@contrib-type=\"author\"]/name")));
    article.setEditors(readEditors(readNodeList(
        "/article/front/article-meta/contrib-group/contrib[@contrib-type=\"editor\"]/name")));

    // TODO Actually this simple, or can it have more structure?
    List<String> collaborativeAuthors = readTextList("/article/front/article-meta/contrib-group/contrib[@contrib-type=\"author\"]/collab");
    collaborativeAuthors = Lists.newArrayList(collaborativeAuthors); // copy to simpler implementation
    article.setCollaborativeAuthors(collaborativeAuthors);
    article.setUrl(buildUrl());

    // TODO Finish implementing

  }

  /**
   * @return the appropriate value for the rights property of {@link Article}, based on the
   *     article XML.
   */
  private String buildRights() {
    StringBuilder rightsStr = new StringBuilder();
    rightsStr.append(readString("/article/front/article-meta/permissions/copyright-holder"))
        .append(". ")
        .append(readString("/article/front/article-meta/permissions/license/license-p"));
    return rightsStr.toString();
  }

  /**
   * @return the appropriate value for the pages property of {@link Article}, based on the
   *     article XML.
   */
  private String buildPages() {
    String pageCount = readString("/article/front/article-meta/counts/page-count/@count");
    if (pageCount == null || pageCount.isEmpty()) {
      return "";
    } else {
      return "1-" + pageCount;
    }
  }

  /**
   * @return the name of the journal the article was published in
   */
  private String buildJournal() {
    String result;
    String journalId = readString(
        "/article/front/journal-meta/journal-id[@journal-id-type='nlm-ta']");
    if (journalId != null && !journalId.isEmpty()) {
      result = journalId;
    } else {
      result = readString("/article/front/journal-meta/journal-title-group/journal-title");
    }
    return result == null ? "" : result;
  }

  /**
   * @return a valid URL to the article (base on the DOI)
   */
  private String buildUrl() {
    String doi = readString("/article/front/article-meta/article-id[@pub-id-type = 'doi']");
    return "http://dx.doi.org/" + URLEncoder.encode(doi);
  }

  /**
   * Build a description field by partially reconstructing the node's content as XML. The output is text content between
   * the node's two tags, including nested XML tags but not this node's outer tags. Nested tags show only the node name;
   * their attributes are deleted. Text nodes containing only whitespace are deleted.
   *
   * @param node the description node
   * @return the marked-up description
   */
  private static String buildDescription(Node node) {
    StringBuilder nodeContent = new StringBuilder();
    buildDescription(nodeContent, node);
    return nodeContent.toString();
  }

  /*
   * Recursive helper method for buildDescription(Node).
   */
  private static void buildDescription(StringBuilder nodeContent, Node node) {
    List<Node> children = NodeListAdapter.wrap(node.getChildNodes());
    for (Node child : children) {
      switch (child.getNodeType()) {
        case Node.TEXT_NODE:
          String text = child.getNodeValue();
          if (!CharMatcher.WHITESPACE.matchesAllOf(text)) {
            nodeContent.append(text);
          }
          break;
        case Node.ELEMENT_NODE:
          String nodeName = child.getNodeName();
          nodeContent.append('<').append(nodeName).append('>');
          buildDescription(nodeContent, child);
          nodeContent.append("</").append(nodeName).append('>');
          break;
      }
    }
  }

  private String parseLanguage(String language) {
    if (language == null) {
      log.warn("Language not specified in article XML; defaulting to English");
      return "en"; // Formerly hard-coded for all articles, so it's the most sensible default
    }
    return language.toLowerCase();
  }

  private Date parseDate(Node dateNode) throws XmlContentException {
    int year, month, day;
    try {
      year = Integer.parseInt(readString("year", dateNode));
      month = Integer.parseInt(readString("month", dateNode));
      day = Integer.parseInt(readString("day", dateNode));
    } catch (NumberFormatException e) {
      throw new XmlContentException("Expected numbers for date fields", e);
    }
    Calendar date = GregorianCalendar.getInstance();

    // Need to call clear to clear out fields we don't set below (like milliseconds).
    date.clear();

    // TODO: figure out if we want to set time zone.  I'm leaving it out for now
    // so that the tests will pass in all locales (the date returned by this method
    // will be in the same time zone as the ones used in the tests).
//    date.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));

    // Note that Calendar wants month to be zero-based.
    date.set(year, month - 1, day, 0, 0, 0);
    return date.getTime();
  }

  private Set<String> parseArticleTypes(Collection<String> articleTypeText) throws XmlContentException {
    Set<String> articleTypes = Sets.newHashSet(articleTypeText);
    if (!VALID_ARTICLE_TYPES.containsAll(articleTypes)) {
      Set<String> invalidTypes = Sets.difference(articleTypes, VALID_ARTICLE_TYPES);
      throw new XmlContentException("Contains invalid article type: " + invalidTypes);
    }
    return articleTypes;
  }

  private List<ArticleAuthor> readAuthors(List<Node> authorNodes) throws XmlContentException {
    List<ArticleAuthor> authors = Lists.newArrayListWithCapacity(authorNodes.size());
    for (Node authorNode : authorNodes) {
      ArticleAuthor author = parsePersonName(authorNode).copyTo(new ArticleAuthor());
      authors.add(author);
    }
    return authors;
  }

  private List<ArticleEditor> readEditors(List<Node> editorNodes) throws XmlContentException {
    List<ArticleEditor> editors = Lists.newArrayListWithCapacity(editorNodes.size());
    for (Node editorNode : editorNodes) {
      ArticleEditor editor = parsePersonName(editorNode).copyTo(new ArticleEditor());
      editors.add(editor);
    }
    return editors;
  }

  private List<CitedArticle> parseCitations(List<Node> citationNodes) throws XmlContentException {
    List<CitedArticle> citations = Lists.newArrayListWithCapacity(citationNodes.size());
    int key = 1;
    for (Node citationNode : citationNodes) {
      CitedArticle citation = new CitedArticleXml(citationNode).build(new CitedArticle());
      citation.setKey(Integer.toString(key++));
      citations.add(citation);
    }
    return citations;
  }

}
