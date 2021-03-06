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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.ambraproject.rhino.content.xml.XpathReader;
import org.ambraproject.rhino.util.NodeListAdapter;
import org.ambraproject.rhino.util.StringReplacer;
import org.ambraproject.rhino.view.article.author.AuthorRole;
import org.ambraproject.rhino.view.article.author.AuthorView;
import org.ambraproject.rhino.view.article.author.Orcid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpressionException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contains logic for extracting author information from article XML.
 * <p/>
 * TODO: store all this data in the database instead at ingest time, and then destroy this class with prejudice.
 */
public final class AuthorsXmlExtractor {

  private static final Logger log = LoggerFactory.getLogger(AuthorsXmlExtractor.class);

  private static final StringReplacer MARKUP_REPLACER = StringReplacer.builder()
      .replaceRegex("<corresp(.*?)>", "")
      .replaceRegex("</corresp>", "")
      .replaceRegex("<email(?:" +
              "(?:\\s+xmlns:xlink\\s*=\\s*\"http://www.w3.org/1999/xlink\"\\s*)|" +
              "(?:\\s+xlink:type\\s*=\\s*\"simple\"\\s*)" +
              ")*>(.*?)</email>",
          "<a href=\"mailto:$1\">$1</a>")
      .replaceRegex("^E-mail:", "<span class=\"email\">* E-mail:</span>")
      .replaceRegex("^\\* E-mail:", "<span class=\"email\">* E-mail:</span>")
      .replaceRegex("\\*To whom", "<span class=\"email\">*</span>To whom")
      .replaceRegex("\\* To whom", "<span class=\"email\">*</span>To whom")
      .replaceRegex("<sec(?:.*)*>", "")
      .replaceRegex("</sec>", "")
      .replaceRegex("<list-item>", "\n<li>")
      .replaceRegex("</list-item>", "</li>")
      .replaceRegex("</list>", "</ul>")
      .replaceRegex("<list(\\s+list-type=\"bullet\")?>", "<ul class=\"bulletlist\">")
      .replaceRegex("<list\\s+list-type=\"(.*)\">", "<ol class=\"$1\">")
      .replaceRegex("<list(?:.*)*>", "<ul class=\"bulletlist\">")
      .replaceRegex("<title(?:.*)*>", "")
      .replaceRegex("<body[^>]*>", "")
      .replaceRegex("</body>", "")
      .build();

  private final static String COMPETING_INTERESTS_XPATH = "//fn[@fn-type='conflict']";
  private final static String AUTHOR_CONTRIBUTIONS_XPATH = "//author-notes/fn[@fn-type='con']";

  private final Document doc;
  private final XpathReader xpath;

  private final ImmutableMap<String, String> affiliateMap;
  private final ImmutableMap<String, String> addressMap;
  private final ImmutableMap<String, String> otherFootnotesMap;

  private AuthorsXmlExtractor(Document doc, XpathReader xpath) throws XPathException {
    this.doc = Preconditions.checkNotNull(doc);
    this.xpath = Preconditions.checkNotNull(xpath);

    affiliateMap = ImmutableMap.copyOf(getAffiliateMap(doc, xpath));
    addressMap = ImmutableMap.copyOf(getAddressMap(doc, xpath));
    otherFootnotesMap = ImmutableMap.copyOf(getOtherFootnotesMap(doc, xpath));
  }

  /**
   * Retrieves the authors as {@link AuthorView}s from article XML.
   *
   * @param doc   parsed representation of the article XML
   * @param xpath XpathReader to use to process xpath expressions
   * @return list of AuthorView objects
   */
  public static List<AuthorView> getAuthors(Document doc, XpathReader xpath) throws XPathException {
    return new AuthorsXmlExtractor(doc, xpath).buildAuthors();
  }

  private List<AuthorView> buildAuthors() throws XPathException {
    List<AuthorView> list = new ArrayList<>();

    //Get all the authors
    NodeList authorList = xpath.selectNodes(doc, "/article/front/article-meta/contrib-group/contrib[@contrib-type='author']");

    for (int i = 0; i < authorList.getLength(); i++) {
      Node authorNode = authorList.item(i);

      //Create temp author document fragment to search out of
      DocumentFragment authorDoc = doc.createDocumentFragment();

      //I thought this strange, appendChild actually moves the node in the case of document fragment
      //hence below I clone to keep the original DOM intact.
      //re: http://docs.oracle.com/javase/1.4.2/docs/api/org/w3c/dom/Node.html#appendChild%28org.w3c.dom.Node%29
      authorDoc.appendChild(authorNode.cloneNode(true));

      Node surNameNode = xpath.selectNode(authorDoc, "./contrib/name/surname");
      Node givenNameNode = xpath.selectNode(authorDoc, "./contrib/name/given-names");
      Node collabNameNode = xpath.selectNode(authorDoc, "//collab");
      Node behalfOfNode = xpath.selectNode(authorDoc, "//on-behalf-of");
      NodeList otherFootnotesNodeList = xpath.selectNodes(authorDoc, "//xref[@ref-type='fn']");

      //Sometimes, an author is not a person, but a collab
      //Note:10.1371/journal.pone.0032315
      if (surNameNode == null && givenNameNode == null) {
        if (collabNameNode != null) {
          //If current node is a collab author.  Make sure previous author
          //Is not marked as "on behalf of"  If so, we can ignore this collab
          //It is assumed this collab contains the same text as the value of the
          //Previous authors "on behalf of" node
          if (list.size() > 0) {
            if (list.get(list.size() - 1).getOnBehalfOf() != null) {

              //Craziness ensues here.  Previous author has "on behalf of", lets append any
              //footnotes from this contrib to that author!
              for (int a = 0; a < otherFootnotesNodeList.getLength(); a++) {
                Node node = otherFootnotesNodeList.item(a);

                if (node.getAttributes().getNamedItem("rid") != null) {
                  String id = node.getAttributes().getNamedItem("rid").getTextContent();
                  String value = otherFootnotesMap.get(id);

                  if (value != null) {
                    AuthorView av = list.get(list.size() - 1);

                    //This may look a bit odd, but because the AuthorView is immutable
                    //I have to create a new copy to change any values
                    List<String> footnotes = new ArrayList<>();
                    footnotes.addAll(av.getCustomFootnotes());

                    value = fixPilcrow(value, false);

                    footnotes.add(value);

                    list.set(list.size() - 1,
                             av.toBuilder()
                            .setCustomFootnotes(footnotes)
                            .build());
                  }
                }
              }

              break;
            }
          }
        }

        givenNameNode = collabNameNode;
      }

      // If both of these are null then don't bother to add
      if (surNameNode == null && givenNameNode == null) {
        continue;
      }

      AuthorView author = getAuthorView(authorDoc, surNameNode, givenNameNode, behalfOfNode, otherFootnotesNodeList);
      list.add(author);
    }

    return list;
  }

  private AuthorView getAuthorView(DocumentFragment authorDoc,
                                   Node surNameNode,
                                   Node givenNameNode,
                                   Node behalfOfNode,
                                   NodeList otherFootnotesNodeList)
      throws XPathException {
    Node suffixNode = xpath.selectNode(authorDoc, "//name/suffix");
    Node equalContribNode = xpath.selectNode(authorDoc, "//@equal-contrib");
    Node deceasedNode = xpath.selectNode(authorDoc, "//@deceased");
    Node corresAuthorNode = xpath.selectNode(authorDoc, "//xref[@ref-type='corresp']");
    NodeList addressList = xpath.selectNodes(authorDoc, "//xref[@ref-type='fn']/sup[contains(text()[1],'¤')]/..");
    NodeList affList = xpath.selectNodes(authorDoc, "//xref[@ref-type='aff']");
    Node orcidNode = xpath.selectNode(authorDoc, "./contrib/contrib-id[@contrib-id-type='orcid']");
    NodeList roleNodes = xpath.selectNodes(authorDoc, "//role");

    // Either surname or givenName can be blank
    String surname = (surNameNode == null) ? null : surNameNode.getTextContent();
    String givenName = (givenNameNode == null) ? null : givenNameNode.getTextContent();
    String suffix = (suffixNode == null) ? null : suffixNode.getTextContent();
    String onBehalfOf = (behalfOfNode == null) ? null : behalfOfNode.getTextContent();

    boolean equalContrib = (equalContribNode != null);
    boolean deceased = (deceasedNode != null);
    boolean relatedFootnote = false;

    String corresponding = null;

    List<String> currentAddresses = new ArrayList<>();
    for (int a = 0; a < addressList.getLength(); a++) {
      Node addressNode = addressList.item(a);

      if (addressNode.getAttributes().getNamedItem("rid") != null) {
        String fnId = addressNode.getAttributes().getNamedItem("rid").getTextContent();
        String curAddress = addressMap.get(fnId);

        //A fix for PBUG-153, sometimes addresses are null because of weird XML
        if (curAddress == null) {
          log.warn("No found current-aff footnote found for fnID: {}", fnId);
        } else {
          if (currentAddresses.size() > 0) {
            //If the current address is already defined, remove "current" text from subsequent
            //addresses
            currentAddresses.add(fixCurrentAddress(curAddress));
          } else {
            currentAddresses.add(curAddress);
          }
        }
      }
    }

    //Footnotes
    //Note this web page for notes on author footnotes:
    //http://wiki.plos.org/pmwiki.php/Publications/FootnoteSymbolOrder
    List<String> otherFootnotes = new ArrayList<>();
    for (int a = 0; a < otherFootnotesNodeList.getLength(); a++) {
      Node node = otherFootnotesNodeList.item(a);

      if (node.getAttributes().getNamedItem("rid") != null) {
        String id = node.getAttributes().getNamedItem("rid").getTextContent();
        String value = otherFootnotesMap.get(id);

        if (value != null) {
          value = fixPilcrow(value, true);
          //If the current footnote is also referenced by another contrib
          //We want to notify the end user of the relation
          if (hasRelatedFootnote(doc, xpath, id)) {
            relatedFootnote = true;
          }

          otherFootnotes.add(value);
        }
      }
    }

    if (corresAuthorNode != null) {
      Node attr = corresAuthorNode.getAttributes().getNamedItem("rid");

      if (attr == null) {
        log.warn("No rid attribute found for xref ref-type=\"corresp\" node.");
      } else {
        String rid = attr.getTextContent();

        Node correspondAddrNode = xpath.selectNode(doc, "//author-notes/corresp[@id='" + rid + "']");

        if (correspondAddrNode == null) {
          log.warn("No node found for corrsponding author: author-notes/corresp[@id='\" + rid + \"']");
        } else {
          // Store the entire corresponding author string, even though it's not specific to this author.
          // It's meant to support the author pop-up. See getCorrespondingAuthorList for the split-up list.
          corresponding = transFormCorresponding(correspondAddrNode);
        }
      }
    }

    List<String> affiliations = new ArrayList<>();

    // Build a list of affiliations for this author
    for (int j = 0; j < affList.getLength(); j++) {
      Node anode = affList.item(j);

      if (anode.getAttributes().getNamedItem("rid") != null) {
        String affId = anode.getAttributes().getNamedItem("rid").getTextContent();
        String affValue = affiliateMap.get(affId);

        //A fix for PBUG-149, sometimes we get wacky XML.  This should handle it so at least the
        //List returned by this method is well structured
        if (affValue != null) {
          affiliations.add(affValue);
        }
      }
    }

    Orcid orcid = (orcidNode != null) ? buildOrcid(orcidNode) : null;
    List<AuthorRole> roles = (roleNodes != null) ? buildRoles(roleNodes) : null;

    return AuthorView.builder()
        .setGivenNames(givenName)
        .setSurnames(surname)
        .setSuffix(suffix)
        .setOnBehalfOf(onBehalfOf)
        .setOrcid(orcid)
        .setRoles(roles)
        .setEqualContrib(equalContrib)
        .setDeceased(deceased)
        .setRelatedFootnote(relatedFootnote)
        .setCorresponding(corresponding)
        .setCurrentAddresses(currentAddresses)
        .setAffiliations(affiliations)
        .setCustomFootnotes(otherFootnotes)
        .build();
  }

  private Orcid buildOrcid(Node orcidNode) {
    String value = orcidNode.getTextContent();
    Node authenticatedNode = orcidNode.getAttributes().getNamedItem("authenticated");
    boolean authenticated = (authenticatedNode != null) &&
        Boolean.TRUE.toString().equalsIgnoreCase(authenticatedNode.getNodeValue());
    return new Orcid(value, authenticated);
  }

  private List<AuthorRole> buildRoles(NodeList roleNodes) {
    return NodeListAdapter.wrap(roleNodes).stream().map((Node roleNode) -> {
      String content = roleNode.getTextContent();
      Node contentTypeNode = roleNode.getAttributes().getNamedItem("content-type");
      String contentType = (contentTypeNode == null) ? null : contentTypeNode.getNodeValue();
      return new AuthorRole(content, contentType);
    }).collect(Collectors.toList());
  }

  /**
   * Grab all affiliations and put them into their own map
   *
   * @param doc   the article XML document
   * @param xpath XpathReader to use to process xpath expressions
   * @return a Map of affiliate IDs and values
   */
  private static Map<String, String> getAffiliateMap(Document doc, XpathReader xpath) throws XPathException {
    Map<String, String> affiliateMap = new LinkedHashMap<>();

    NodeList affiliationNodeList = xpath.selectNodes(doc, "//aff");

    //Map all affiliation id's to their affiliation strings
    for (int a = 0; a < affiliationNodeList.getLength(); a++) {
      Node node = affiliationNodeList.item(a);
      // Not all <aff>'s have the 'id' attribute.
      String id = (node.getAttributes().getNamedItem("id") == null) ? "" :
          node.getAttributes().getNamedItem("id").getTextContent();

      log.debug("Found affiliation node: {}", id);

      // Not all <aff> id's are affiliations.
      if (id.startsWith("aff")) {
        DocumentFragment df = doc.createDocumentFragment();
        //because of a org.w3c.Document.dom.Document peculiarity, simple appellation will strip it from the source and
        //cause bugs, so we need cloning technology
        df.appendChild(node.cloneNode(true));

        StringBuilder res = new StringBuilder();

        if (xpath.selectNode(df, "//institution") != null) {
          res.append(xpath.selectString(df, "//institution"));
        }

        if (xpath.selectNode(df, "//addr-line") != null) {
          if (res.length() > 0) {
            res.append(" ");
          }
          res.append(xpath.selectString(df, "//addr-line"));
        }

        affiliateMap.put(id, res.toString());
      }
    }

    return affiliateMap;
  }

  /**
   * Grab all addresses and put them into their own map
   *
   * @param doc   the article XML document
   * @param xpath XpathReader to use to process xpath expressions
   * @return a Map of address IDs and values
   */
  private static Map<String, String> getAddressMap(Document doc, XpathReader xpath) throws XPathException {
    Map<String, String> addressMap = new HashMap<>();

    //Grab all the Current address information and place them into a map
    NodeList currentAddressNodeList = xpath.selectNodes(doc, "//fn[@fn-type='current-aff']");
    for (int a = 0; a < currentAddressNodeList.getLength(); a++) {
      Node node = currentAddressNodeList.item(a);
      String id = (node.getAttributes().getNamedItem("id") == null) ? "" :
          node.getAttributes().getNamedItem("id").getTextContent();

      log.debug("Current address node: {}", id);

      DocumentFragment df = doc.createDocumentFragment();
      df.appendChild(node);

      String address = xpath.selectString(df, "//p");
      addressMap.put(id, address);
    }

    return addressMap;
  }

  /**
   * Grab all footnotes and put them into their own map
   *
   * @param doc   the article XML document
   * @param xpath XpathReader to use to process xpath expressions
   * @return a Map of footnote IDs and values
   */
  private static Map<String, String> getOtherFootnotesMap(Document doc, XpathReader xpath) throws XPathException {
    Map<String, String> otherFootnotesMap = new HashMap<>();

    //Grab all 'other' footnotes and put them into their own map
    NodeList footnoteNodeList = xpath.selectNodes(doc, "//fn[@fn-type='other']");

    for (int a = 0; a < footnoteNodeList.getLength(); a++) {
      Node node = footnoteNodeList.item(a);
      // Not all <aff>'s have the 'id' attribute.
      String id = (node.getAttributes().getNamedItem("id") == null) ? "" :
          node.getAttributes().getNamedItem("id").getTextContent();

      log.debug("Found footnote node: {}", id);

      DocumentFragment df = doc.createDocumentFragment();
      df.appendChild(node);

      String footnote;
      try {
        footnote = getAsXMLString(xpath.selectNode(df, "//p"));
      } catch (TransformerException e) {
        throw new RuntimeException(e);
      }
      otherFootnotesMap.put(id, footnote);
    }

    return otherFootnotesMap;
  }

  private static String getAsXMLString(Node node) throws TransformerException {
    final Transformer tf = TransformerFactory.newInstance().newTransformer();
    final StringWriter stringWriter = new StringWriter();

    tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    tf.transform(new DOMSource(node), new StreamResult(stringWriter));

    return stringWriter.toString();
  }

  /**
   * Get the author contributions
   *
   * @param doc   the article XML document
   * @param xpath XpathReader to use to process xpath expressions
   * @return the author contributions
   */
  public static List<String> getAuthorContributions(Document doc, XpathReader xpath) throws XPathException {
    return findTextFromNodes(doc, AUTHOR_CONTRIBUTIONS_XPATH, xpath);
  }

  /**
   * Get the competing interests statement
   *
   * @param doc   the article XML document
   * @param xpath XpathReader to use to process xpath expressions
   * @return the competing interests statement
   */
  public static List<String> getCompetingInterests(Document doc, XpathReader xpath) throws XPathException {
    return findTextFromNodes(doc, COMPETING_INTERESTS_XPATH, xpath);
  }

  /**
   * Reformat html embedded into the XML into something more easily styled on the front end
   *
   * @param source      html fragment
   * @param prependHTML if true, append a html snippet for a 'pilcrow' (¶)
   * @return html fragment
   */
  private static String fixPilcrow(String source, boolean prependHTML) {
    String destination;

    if (prependHTML) {
      destination = source.replace("<sup>¶</sup>", "<span class=\"rel-footnote\">¶</span>");
      destination = destination.replaceAll("^<p>¶?\\s*", "<p><span class=\"rel-footnote\">¶</span>");
    } else {
      destination = source.replace("<sup>¶</sup>", "");
      destination = destination.replaceAll("^<p>¶?\\s*", "<p>");
    }

    return destination;
  }

  /**
   * Remove "current" text from an address field
   *
   * @param source text fragment
   * @return text fragment
   */
  private static String fixCurrentAddress(String source) {
    return source.replaceAll("Current\\s[Aa]ddress:\\s*", "");
  }

  /**
   * Check to see if the current footnote is referenced by multiple contribs If the current footnote is also referenced
   * by another contrib We want to notify the end user of the relation
   *
   * @param doc            the document
   * @param xpathExtractor XpathReader to use to process xpath expressions
   * @param rid            the rid to search for, the RID is an attribute of a footnote that attaches a footnote to one
   *                       or many authors
   * @return true if the rid is referenced by contribs more then once
   * @throws XPathException
   */
  private static boolean hasRelatedFootnote(Node doc, XpathReader xpathExtractor, String rid)
      throws XPathException {
    String xpath = "//contrib/xref[@ref-type='fn' and @rid='" + rid + "']";

    log.debug("xpath: {}", xpath);
    NodeList nl = xpathExtractor.selectNodes(doc, xpath);
    log.debug("nodecount: {}", nl.getLength());

    return nl.getLength() > 1;
  }

  /**
   * Kludge for FEND-794, A better ways of doing this?
   * <p/>
   * Reformat html embedded into the XML into something more easily styled on the front end
   *
   * @param correspondAddrNode html node
   * @return html fragment
   */
  private static String transFormCorresponding(Node correspondAddrNode) {
    String corresponding;
    try {
      corresponding = getAsXMLString(correspondAddrNode);
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
    return MARKUP_REPLACER.replace(corresponding);
  }

  /**
   * @param document        a document to search for nodes
   * @param xpathExpression XPath describing the nodes to find
   * @return a list of the text content of the nodes found, or {@code null} if none
   */
  private static List<String> findTextFromNodes(Document document, String xpathExpression,
                                                XpathReader xPath) throws XPathException {

    NodeList nodes;
    try {
      nodes = xPath.selectNodes(document, xpathExpression);
      //todo: this should catch explicit "node missing" exceptions instead of generic XPathException
    } catch (XPathExpressionException ex) {
      log.error("Error occurred while gathering text with: " + xpathExpression, ex);
      return null;
    }

    List<String> text = new ArrayList<>(nodes.getLength());

    for (int i = 0; i < nodes.getLength(); i++) {
      text.add(nodes.item(i).getTextContent());
    }

    return text;
  }

  /**
   * Split the document-level list of corresponding authors into parts and do some string-hacking. Returns
   * ready-to-display HTML. This is far more display logic than we would like, but it was lifted directly from Ambra.
   */
  public static List<String> getCorrespondingAuthorList(Document document, XpathReader xpath) throws XPathException {
    Node corresAuthorNode = xpath.selectNode(document, "//corresp");
    if (corresAuthorNode == null) return ImmutableList.of();
    String r = transFormCorresponding(corresAuthorNode);

    //Remove prepending text
    r = r.replaceAll("<span.*?/span>", "");
    r = r.replaceFirst(".*?[Ee]-mail:", "");

    //Remove extra carriage return
    r = r.replaceAll("\\n", "");

    //Split on "<a" as the denotes a new email address
    String[] emails = r.split("(?=<a)");

    List<String> result = new ArrayList<>();
    for (int a = 0; a < emails.length; a++) {
      if (emails[a].trim().length() > 0) {
        String email = emails[a];
        //Remove ; and "," from address
        email = email.replaceAll("[,;]", "");
        email = email.replaceAll("[Ee]mail:", "");
        email = email.replaceAll("[Ee]-mail:", "");
        result.add(email.trim());
      }
    }

    return result;
  }

}
