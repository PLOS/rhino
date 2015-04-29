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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAuthor;
import org.ambraproject.models.ArticleEditor;
import org.ambraproject.models.ArticleRelationship;
import org.ambraproject.models.CitedArticle;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.util.NodeListAdapter;
import org.ambraproject.rhino.util.StringReplacer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.annotation.Nullable;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * An NLM-format XML document that can be "ingested" to build an {@link Article} object.
 */
public class ArticleXml extends AbstractArticleXml<Article> {

  private static final Logger log = LoggerFactory.getLogger(ArticleXml.class);

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
   * Find each node within this object's XML whose name is expected to be associated with an asset entity.
   *
   * @return the list of asset nodes
   */
  public AssetNodesByDoi findAllAssetNodes() {
    // Find all nodes of an asset type and map them by DOI
    List<Node> rawNodes = readNodeList(ASSET_EXPRESSION);
    ListMultimap<String, Node> nodeMap = LinkedListMultimap.create(rawNodes.size());
    for (Node node : rawNodes) {
      String assetDoi = getAssetDoi(node);
      if (assetDoi != null) {
        nodeMap.put(assetDoi, node);
      } else {
        findNestedDoi(node, nodeMap);
      }
    }

    // Replace <graphic> nodes without changing keys or iteration order
    for (Map.Entry<String, Node> entry : nodeMap.entries()) {
      Node node = entry.getValue();
      if (node.getNodeName().equals(GRAPHIC)) {
        entry.setValue(replaceGraphicNode(node));
      }
    }

    return new AssetNodesByDoi(nodeMap);
  }

  public Node extractFrontMatter() {
    return readNode("/article/front");
  }

  /**
   * Return the node that should represent the asset for a "graphic" node.
   *
   * @param node a node such that {@code node.getNodeName().equals(GRAPHIC)}
   * @return the corresponding asset node
   */
  private static Node replaceGraphicNode(Node node) {
    Node parent = node.getParentNode();
    if (GRAPHIC_NODE_PARENTS.contains(parent.getNodeName())) {
      return parent;
    }
    if (parent.getNodeName().equals(ALTERNATIVES)) {
      return parent.getParentNode();
    }
    return node;
  }

  /**
   * Try to find a DOI at a special position within an XML node and, if it's found, insert it into the node map at that
   * key.
   *
   * @param outerNode an asset node such that {@code getAssetDoi(outerNode) == null}
   * @param nodeMap   the map to modify if a nested DOI is found
   */
  private void findNestedDoi(Node outerNode, Multimap<String, Node> nodeMap) {
    Preconditions.checkNotNull(nodeMap);

    // Currently, the only special case handled here is
    //   <table-wrap> ... <graphic xlink:href="..." /> ... </table-wrap>
    // See case pone.0012008 (asset 10.1371/journal.pone.0012008.t002) in the test suite.
    if (TABLE_WRAP.equals(outerNode.getNodeName())) {
      Node graphicNode = readNode("descendant::" + GRAPHIC, outerNode);
      if (graphicNode != null) {
        String doi = getAssetDoi(graphicNode);
        if (doi != null) {
          nodeMap.put(doi, outerNode);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Article build(Article article) throws XmlContentException {
    article.setDoi(readDoi().getKey());
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

  private void setFromXml(final Article article) throws XmlContentException {
    article.setTitle(buildXmlExcerpt(readNode("/article/front/article-meta/title-group/article-title")));
    article.seteIssn(checkEissn(readString("/article/front/journal-meta/issn[@pub-type=\"epub\"]")));
    article.setDescription(buildXmlExcerpt(findAbstractNode()));

    String rights = readString("/article/front/article-meta/copyright-statement");
    if (rights == null) {
      rights = buildRights(
          readString("/article/front/article-meta/permissions/copyright-holder"),
          readString("/article/front/article-meta/permissions/license/license-p"));
    }
    article.setRights(rights);

    article.setPages(buildPages(readString("/article/front/article-meta/counts/page-count/@count")));
    article.seteLocationId(readString("/article/front/article-meta/elocation-id"));
    article.setVolume(readString("/article/front/article-meta/volume"));
    article.setIssue(readString("/article/front/article-meta/issue"));
    article.setJournal(buildJournal());
    article.setPublisherName(readString("/article/front/journal-meta/publisher/publisher-name"));
    article.setPublisherLocation(readString("/article/front/journal-meta/publisher/publisher-loc"));

    article.setLanguage(parseLanguage(readString("/article/@xml:lang")));
    article.setDate(parseDate(readNode("/article/front/article-meta/pub-date[@pub-type=\"epub\"]")));

    // We have to be careful when setting Article properties that have a one-to-many relationship with entities
    // that are marked 'cascade="all-delete-orphans"' in the hibernate config.  This is especially important
    // during reingestion.  If we call the setter of such a property, and it's already populated, then the old
    // entries are garbage-collected and eventually deleted from the database.  This is usually not what we
    // want, and it leads to bugs.

    // TODO: this can probably be made more elegant with lambdas once we are on Java 8.

    final Set<String> newTypes = buildArticleTypes();
    setDeletableChildrenRelationship(article, article.getTypes(), newTypes, new Runnable() {
      @Override
      public void run() {
        article.setTypes(newTypes);
      }
    });

    final List<CitedArticle> newCitedArticles = parseCitations(readNodeList("/article/back/ref-list/ref"));
    setDeletableChildrenRelationship(article, article.getCitedArticles(), newCitedArticles, new Runnable() {
      @Override
      public void run() {
        article.setCitedArticles(newCitedArticles);
      }
    });

    final List<ArticleAuthor> newAuthors = readAuthors(readNodeList(
        "/article/front/article-meta/contrib-group/contrib[@contrib-type=\"author\"]/name"));
    setDeletableChildrenRelationship(article, article.getAuthors(), newAuthors, new Runnable() {
      @Override
      public void run() {
        article.setAuthors(newAuthors);
      }
    });

    final List<ArticleEditor> newEditors = readEditors(readNodeList(
        "/article/front/article-meta/contrib-group/contrib[@contrib-type=\"editor\"]/name"));
    setDeletableChildrenRelationship(article, article.getEditors(), newEditors, new Runnable() {
      @Override
      public void run() {
        article.setEditors(newEditors);
      }
    });

    final List<String> newCollaborativeAuthors = parseCollaborativeAuthors(readNodeList(
        "/article/front/article-meta/contrib-group/contrib[@contrib-type=\"author\"]/collab"));
    setDeletableChildrenRelationship(article, article.getCollaborativeAuthors(), newCollaborativeAuthors,
        new Runnable() {
          @Override
          public void run() {
            article.setCollaborativeAuthors(newCollaborativeAuthors);
          }
        }
    );

    article.setUrl(buildUrl(readString("/article/front/article-meta/article-id[@pub-id-type = 'doi']")));
  }

  /**
   * Safely sets a property of an article that has a one-to-many relationship, and is automatically deleted when
   * dissociated from the parent.  The collection will only be set if it differs from the values already defined.
   * <p/>
   * For example, instead of <pre>article.setFoo(newStuff);</pre>
   * you should instead do
   * <pre>
   *   setDeletableChildrenRelationship(article, article.getFoo(), newStuff, new Runnable() {
   *     public void run() {
   *       article.setFoo(newStuff);
   *     }
   *   }
   * </pre>
   *
   * @param article        parent object
   * @param existingValues existing values obtained by calling the getter on article
   * @param newValues      new values that will be set if they differ from existingValues
   * @param setter         encapsulates the property's setter method, which will be called with newValues when run
   */
  private void setDeletableChildrenRelationship(Article article, @Nullable Collection existingValues,
                                                Collection newValues, Runnable setter) {
    if (!newValues.equals(existingValues)) {
      if (existingValues != null) {

        // This is the safe and recommended way to work with a hibernate all-delete-orphan property.  Instead
        // of calling the setter with the new collection, work with the existing collection, which is already
        // imbued with hibernate magic.
        existingValues.clear();
        existingValues.addAll(newValues);
      } else {
        setter.run();
      }
    }
  }

  /**
   * Queries for where the article abstract is found, ordered by priority. If more than one matches to a node, the first
   * one should be stored as the article abstract.
   */
  private static final ImmutableList<String> QUERIES_FOR_ABSTRACT = ImmutableList.of(
      "/article/front/article-meta/abstract[@abstract-type=\"toc\"]",
      "/article/front/article-meta/abstract[@abstract-type=\"summary\"]",
      "/article/front/article-meta/abstract");

  /**
   * @return the node containing the article abstract
   */
  private Node findAbstractNode() {
    for (String query : QUERIES_FOR_ABSTRACT) {
      Node node = readNode(query);
      if (node != null) {
        return node;
      }
    }
    return null;
  }

  private static String checkEissn(String eissn) throws XmlContentException {
    if (eissn == null) {
      throw new XmlContentException("Required eIssn is omitted");
    }
    return eissn;
  }

  /**
   * @return the appropriate value for the rights property of {@link Article}, based on the article XML.
   */
  private static String buildRights(String holder, String license) throws XmlContentException {
    if (license == null) {
      throw new XmlContentException("Required license statement is omitted");
    }

    // pmc2obj-v3.xslt lines 179-183
    String rights = (holder == null) ? license : (holder + ". " + license);
    return rights.trim();
  }

  /**
   * @return the appropriate value for the pages property of {@link Article}, based on the article XML.
   */
  private static String buildPages(String pageCount) {
    if (Strings.isNullOrEmpty(pageCount)) {
      return null;
    } else {
      return "1-" + pageCount;
    }
  }

  /**
   * @return the name of the journal the article was published in
   */
  private String buildJournal() {

    // pmc2obj-v3.xslt lines 308-311
    String result;
    String journalId = readString(
        "/article/front/journal-meta/journal-id[@journal-id-type='nlm-ta']");
    if (!Strings.isNullOrEmpty(journalId)) {
      result = journalId;
    } else {
      result = readString("/article/front/journal-meta/journal-title-group/journal-title");
    }
    return result == null ? "" : result;
  }

  /**
   * @return a valid URL to the article (base on the DOI)
   */
  private static String buildUrl(String doi) {
    return "http://dx.doi.org/" + URLEncoder.encode(doi);
  }

  /**
   * @return set of article type strings for the article
   */
  private Set<String> buildArticleTypes() {

    // pmc2obj-v3.xslt lines 93-96
    Set<String> articleTypes = Sets.newHashSet();
    articleTypes.add("http://rdf.plos.org/RDF/articleType/"
        + readString("/article/@article-type"));
    List<String> otherTypes = readTextList("/article/front/article-meta/article-categories/"
        + "subj-group[@subj-group-type = 'heading']/subject");
    for (String otherType : otherTypes) {
      otherType = uriEncode(otherType);
      otherType = SLASH_ESCAPE.replace(otherType); // uriEncode leaves slashes alone, but we actually want them escaped
      articleTypes.add("http://rdf.plos.org/RDF/articleType/" + otherType); // TODO PLOS-specific
    }
    return articleTypes;
  }

  private static final StringReplacer SLASH_ESCAPE = StringReplacer.builder()
      .replaceExact("/", String.format("%%%H", '/'))
      .build();

  /**
   * Build a field of XML text by partially reconstructing the node's content. The output is text content between the
   * node's two tags, including nested XML tags but not this node's outer tags. Nested tags show only the node name;
   * their attributes are deleted. Text nodes containing only whitespace are deleted.
   *
   * @param node the description node
   * @return the marked-up description
   */
  private static String buildXmlExcerpt(Node node) {
    return (node == null) ? null : buildTextWithMarkup(node);
  }


  private static String parseLanguage(String language) {
    if (language == null) {
      log.warn("Language not specified in article XML; defaulting to English");
      return "en"; // Formerly hard-coded for all articles, so it's the most sensible default
    }
    return language.toLowerCase();
  }

  private Date parseDate(Node dateNode) throws XmlContentException {
    int year, month, day;
    try {
      year = Integer.parseInt(readString("child::year", dateNode));
      month = Integer.parseInt(readString("child::month", dateNode));
      day = Integer.parseInt(readString("child::day", dateNode));
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

  private List<CitedArticle> parseCitations(List<Node> refNodes) throws XmlContentException {
    List<CitedArticle> citations = Lists.newArrayListWithCapacity(refNodes.size());
    for (Node refNode : refNodes) {
      CitedArticle citation = new CitedArticle();
      citation.setKey(readString("child::label", refNode));

      Node citationNode = readNode("(child::element-citation|child::mixed-citation|child::nlm-citation)", refNode);
      if (citationNode == null) {
        throw new XmlContentException("All citation (<ref>) nodes expected to contain one of: "
            + "element-citation, mixed-citation, nlm-citation");
      }
      citation = new CitedArticleXml(citationNode).build(citation);
      citations.add(citation);
    }
    return citations;
  }

  /**
   * Convert each collab node to its text content, excluding any text that appears inside a nested "contrib-group"
   * element.
   * <p/>
   * TODO: Find a way to do this with just XPath?
   *
   * @param collabNodes XML nodes representing "collab" elements
   * @return a list of their text content
   */
  private static List<String> parseCollaborativeAuthors(List<Node> collabNodes) {
    List<String> collabStrings = Lists.newArrayListWithCapacity(collabNodes.size());
    for (Node collabNode : collabNodes) {
      StringBuilder text = new StringBuilder();
      for (Node child : NodeListAdapter.wrap(collabNode.getChildNodes())) {
        if (!"contrib-group".equals(child.getNodeName())) {
          text.append(child.getTextContent());
        }
      }
      String result = standardizeWhitespace(text.toString());
      collabStrings.add(result);
    }
    return collabStrings;
  }

  /**
   * Parse {@link ArticleRelationship} objects from XML.
   * <p/>
   * These are returned from here, rather than set in the {@link Article} object during normal parsing, so they can get
   * special handling.
   *
   * @return the article relationships defined by the XML
   */
  public List<ArticleRelationship> parseRelatedArticles() {
    List<Node> relatedArticleNodes = readNodeList("//related-article");
    List<ArticleRelationship> relatedArticles = Lists.newArrayListWithCapacity(relatedArticleNodes.size());
    for (Node relatedArticleNode : relatedArticleNodes) {
      ArticleRelationship relatedArticle = new ArticleRelationship();
      relatedArticle.setType(readString("attribute::related-article-type", relatedArticleNode));
      relatedArticle.setOtherArticleDoi(readHrefAttribute(relatedArticleNode));
      relatedArticles.add(relatedArticle);
    }
    return relatedArticles;
  }

}
