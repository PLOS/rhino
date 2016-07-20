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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.article.ArticleMetadata;
import org.ambraproject.rhino.model.article.AssetMetadata;
import org.ambraproject.rhino.model.article.RelatedArticleLink;
import org.ambraproject.rhino.util.NodeListAdapter;
import org.ambraproject.rhino.util.StringReplacer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URLEncoder;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An NLM-format XML document that can be "ingested" to build an {@link ArticleMetadata} object.
 */
public class ArticleXml extends AbstractArticleXml<ArticleMetadata> {

  private static final Logger log = LoggerFactory.getLogger(ArticleXml.class);

  public ArticleXml(Document xml) {
    super(xml);
  }

  public Document getDocument() {
    return (Document) xml;
  }


  /**
   * @return
   * @throws XmlContentException if the DOI is not present
   */
  public Doi readDoi() throws XmlContentException {
    String doi = readString("/article/front/article-meta/article-id[@pub-id-type=\"doi\"]");
    if (doi == null) {
      throw new XmlContentException("DOI not found");
    }
    return Doi.create(doi);
  }

  public int getRevisionNumber() {
    return 1; // Placeholder value. TODO: Definition TBD
  }

  /**
   * Find each node within this object's XML whose name is expected to be associated with an asset entity.
   *
   * @return the list of asset nodes
   */
  public AssetNodesByDoi findAllAssetNodes() {
    // Find all nodes of an asset type and map them by DOI
    List<Node> rawNodes = readNodeList(ASSET_EXPRESSION);
    ListMultimap<Doi, Node> nodeMap = LinkedListMultimap.create(rawNodes.size());
    for (Node node : rawNodes) {
      Doi assetDoi = getAssetDoi(node);
      if (assetDoi != null) {
        nodeMap.put(assetDoi, node);
      } else {
        findNestedDoi(node, nodeMap);
      }
    }

    // Replace <graphic> nodes without changing keys or iteration order
    for (Map.Entry<Doi, Node> entry : nodeMap.entries()) {
      Node node = entry.getValue();
      if (node.getNodeName().equals(GRAPHIC)) {
        entry.setValue(replaceGraphicNode(node));
      }
    }

    return new AssetNodesByDoi(nodeMap);
  }

  private static final ImmutableSet<String> BODY = ImmutableSet.of("body");
  private static final ImmutableSet<String> BODY_AND_BACK = ImmutableSet.of("body", "back");

  /**
   * Build a deep copy of the article XML document that contains only the {@code &lt;front>} node. The returned document
   * can be read (into a future {@code ArticleXml} object, or by other means) and parsed to get article metadata from
   * the front matter. Metadata from the {@code &lt;back>} node will not be available.
   *
   * @return a copy of the document that contains only the {@code &lt;front>} node
   */
  public Document extractFrontMatter() {
    return truncate(BODY_AND_BACK);
  }

  /**
   * Build a deep copy of the article XML document that contains only the {@code &lt;front>} and {@code &lt;back>} node.
   * The returned document can be read (into a future {@code ArticleXml} object, or by other means) and parsed to get
   * article metadata.
   *
   * @return a copy of the document that contains only the {@code &lt;front>} node
   */
  public Document extractFrontAndBackMatter() {
    return truncate(BODY);
  }

  private Document truncate(Set<String> nodeNamesToRemove) {
    Preconditions.checkNotNull(nodeNamesToRemove);
    Document document = (Document) xml.cloneNode(true);
    for (Node articleNode : NodeListAdapter.wrap(document.getChildNodes())) {
      NodeList childNodes = articleNode.getChildNodes(); // can't use NodeListAdapter because we want to delete during iteration
      for (int i = 0; i < childNodes.getLength(); i++) {
        Node childNode = childNodes.item(i);
        if (nodeNamesToRemove.contains(childNode.getNodeName())) {
          articleNode.removeChild(childNode);
          i--;
        }
      }
    }
    return document;
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
  private void findNestedDoi(Node outerNode, Multimap<Doi, Node> nodeMap) {
    Preconditions.checkNotNull(nodeMap);

    // Currently, the only special case handled here is
    //   <table-wrap> ... <graphic xlink:href="..." /> ... </table-wrap>
    // See case pone.0012008 (asset 10.1371/journal.pone.0012008.t002) in the test suite.
    if (TABLE_WRAP.equals(outerNode.getNodeName())) {
      Node graphicNode = readNode("descendant::" + GRAPHIC, outerNode);
      if (graphicNode != null) {
        Doi doi = getAssetDoi(graphicNode);
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
  public ArticleMetadata build() throws XmlContentException {
    ArticleMetadata.Builder article = ArticleMetadata.builder();
    setConstants(article);
    setFromXml(article);
    return article.build();
  }

  /**
   * Set constant values.
   *
   * @param article the article to modify
   */
  private static void setConstants(ArticleMetadata.Builder article) {
    // These are constants because they are implied by how we get the input
    article.setFormat("text/xml");
  }

  private void setFromXml(final ArticleMetadata.Builder article) throws XmlContentException {
    article.setDoi(readDoi().getName());

    article.setTitle(parseTitle());
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
    article.setPublicationDate(parseDate(readNode("/article/front/article-meta/pub-date[@pub-type=\"epub\"]")));

    article.setTypes(buildArticleTypes());
    article.setAuthors(readPersons(readNodeList(
        "/article/front/article-meta/contrib-group/contrib[@contrib-type=\"author\"]/name")));
    article.setEditors(readPersons(readNodeList(
        "/article/front/article-meta/contrib-group/contrib[@contrib-type=\"editor\"]/name")));
    article.setCollaborativeAuthors(parseCollaborativeAuthors(readNodeList(
        "/article/front/article-meta/contrib-group/contrib[@contrib-type=\"author\"]/collab")));

    article.setUrl(buildUrl(readString("/article/front/article-meta/article-id[@pub-id-type = 'doi']")));

    article.setRelatedArticles(parseRelatedArticles());

    article.setAssets(parseAssets());
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
   * @return the appropriate value for the rights property of {@link ArticleMetadata}, based on the article XML.
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
   * @return the appropriate value for the pages property of {@link ArticleMetadata}, based on the article XML.
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

  private static String buildArticleTypeUri(String articleType) {
    // Represent a article type token as a URI, as required by legacy article type model.
    // TODO: Either refactor URI-based data model, or allow base URI to be configured (no hard-coded "plos.org").
    // This also ensures that we throw a NullPointerException rather than return "http://rdf.plos.org/RDF/articleType/null"
    return "http://rdf.plos.org/RDF/articleType/" + Preconditions.checkNotNull(articleType);
  }

  /**
   * @return set of article type strings for the article
   */
  private Set<String> buildArticleTypes() {

    // pmc2obj-v3.xslt lines 93-96
    Set<String> articleTypes = Sets.newHashSet();
    articleTypes.add(buildArticleTypeUri(readString("/article/@article-type")));
    List<String> otherTypes = readTextList("/article/front/article-meta/article-categories/"
        + "subj-group[@subj-group-type = 'heading']/subject");
    for (String otherType : otherTypes) {
      otherType = uriEncode(otherType);
      otherType = SLASH_ESCAPE.replace(otherType); // uriEncode leaves slashes alone, but we actually want them escaped
      articleTypes.add(buildArticleTypeUri(otherType));
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

  private LocalDate parseDate(Node dateNode) throws XmlContentException {
    int year, month, day;
    try {
      year = Integer.parseInt(readString("child::year", dateNode));
      month = Integer.parseInt(readString("child::month", dateNode));
      day = Integer.parseInt(readString("child::day", dateNode));
    } catch (NumberFormatException e) {
      throw new XmlContentException("Expected numbers for date fields", e);
    }

    return LocalDate.of(year, month, day);
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
   * Parse {@link RelatedArticleLink} objects from XML.
   * <p/>
   * These are returned from here, rather than set in the {@link ArticleMetadata} object during normal parsing, so they
   * can get special handling.
   *
   * @return the article relationships defined by the XML
   */
  public List<RelatedArticleLink> parseRelatedArticles() {
    List<Node> relatedArticleNodes = readNodeList("//related-article");
    List<RelatedArticleLink> relatedArticles = Lists.newArrayListWithCapacity(relatedArticleNodes.size());
    for (Node relatedArticleNode : relatedArticleNodes) {
      String type = readString("attribute::related-article-type", relatedArticleNode);
      String doi = readHrefAttribute(relatedArticleNode);
      RelatedArticleLink relatedArticle = new RelatedArticleLink(type, ArticleIdentifier.create(doi));
      relatedArticles.add(relatedArticle);
    }
    return relatedArticles;
  }

  private List<AssetMetadata> parseAssets() {
    AssetNodesByDoi nodeMap = findAllAssetNodes();
    return nodeMap.getDois().stream().map((Doi assetDoi) -> {
      ImmutableList<Node> nodes = nodeMap.getNodes(assetDoi);
      List<AssetMetadata> assetMetadataList = nodes.stream()
          .map(assetNode -> new AssetXml(assetNode, assetDoi).build())
          .distinct()
          .collect(Collectors.toList());
      if (assetMetadataList.size() > 1) {
        // TODO: It might be okay if the duplicate AssetMetadata objects differ only by contextElement
        throw new XmlContentException("Non-matching duplicate assets with DOI: " + assetDoi);
      }
      return assetMetadataList.get(0);
    }).collect(Collectors.toList());
  }

  public String parseTitle() {
    return buildXmlExcerpt(readNode("/article/front/article-meta/title-group/article-title"));
  }

}
