/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2014 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.service.impl;

import org.ambraproject.rhino.shared.XPathExtractor;
import org.ambraproject.rhino.util.StringReplacer;
import org.ambraproject.util.TextUtils;
import org.ambraproject.views.AuthorView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

  private AuthorsXmlExtractor() {
  }

  /**
   * Retrieves the authors as {@link AuthorView}s from article XML.
   *
   * @param doc   parsed representation of the article XML
   * @param xpath XPathExtractor to use to process xpath expressions
   * @return list of AuthorView objects
   */
  public static List<AuthorView> getAuthors(Document doc, XPathExtractor xpath) {
    ArrayList<AuthorView> list = new ArrayList<AuthorView>();

    if (doc == null) {
      return list;
    }

    try {
      Map<String, String> affiliateMap = getAffiliateMap(doc, xpath);
      Map<String, String> addressMap = getAddressMap(doc, xpath);
      Map<String, String> otherFootnotesMap = getOtherFootnotesMap(doc, xpath);

      //Get all the authors
      NodeList authorList = xpath.selectNodes(doc, "//contrib-group/contrib[@contrib-type='author']");

      for (int i = 0; i < authorList.getLength(); i++) {
        Node authorNode = authorList.item(i);

        //Create temp author document fragment to search out of
        DocumentFragment authorDoc = doc.createDocumentFragment();

        //I thought this strange, appendChild actually moves the node in the case of document fragment
        //hence below I clone to keep the original DOM intact.
        //re: http://docs.oracle.com/javase/1.4.2/docs/api/org/w3c/dom/Node.html#appendChild%28org.w3c.dom.Node%29
        authorDoc.appendChild(authorNode.cloneNode(true));

        Node surNameNode = xpath.selectNode(authorDoc, "//name/surname");
        Node givenNameNode = xpath.selectNode(authorDoc, "//name/given-names");
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
                      List<String> footnotes = new ArrayList<String>();
                      footnotes.addAll(av.getCustomFootnotes());

                      value = fixPilcrow(value, false);

                      footnotes.add(value);

                      list.set(list.size() - 1,
                          AuthorView.builder(av)
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
        if (surNameNode != null || givenNameNode != null) {
          Node suffixNode = xpath.selectNode(authorDoc, "//name/suffix");
          Node equalContribNode = xpath.selectNode(authorDoc, "//@equal-contrib");
          Node deceasedNode = xpath.selectNode(authorDoc, "//@deceased");
          Node corresAuthorNode = xpath.selectNode(authorDoc, "//xref[@ref-type='corresp']");
          NodeList addressList = xpath.selectNodes(authorDoc, "//xref[@ref-type='fn']/sup[contains(text(),'¤')]/..");
          NodeList affList = xpath.selectNodes(authorDoc, "//xref[@ref-type='aff']");

          // Either surname or givenName can be blank
          String surname = (surNameNode == null) ? null : surNameNode.getTextContent();
          String givenName = (givenNameNode == null) ? null : givenNameNode.getTextContent();
          String suffix = (suffixNode == null) ? null : suffixNode.getTextContent();
          String onBehalfOf = (behalfOfNode == null) ? null : behalfOfNode.getTextContent();

          boolean equalContrib = (equalContribNode != null);
          boolean deceased = (deceasedNode != null);
          boolean relatedFootnote = false;

          String corresponding = null;

          List<String> currentAddresses = new ArrayList<String>();
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
          List<String> otherFootnotes = new ArrayList<String>();
          for (int a = 0; a < otherFootnotesNodeList.getLength(); a++) {
            Node node = otherFootnotesNodeList.item(a);

            if (node.getAttributes().getNamedItem("rid") != null) {
              String id = node.getAttributes().getNamedItem("rid").getTextContent();
              String value = otherFootnotesMap.get(id);

              if (value != null) {
                //If the current footnote is also referenced by another contrib
                //We want to notify the end user of the relation
                if (hasRelatedFootnote(doc, xpath, id)) {
                  value = fixPilcrow(value, true);
                  relatedFootnote = true;
                } else {
                  value = fixPilcrow(value, false);
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
                corresponding = TextUtils.getAsXMLString(correspondAddrNode);
                corresponding = transFormCorresponding(corresponding);
              }
            }
          }

          List<String> affiliations = new ArrayList<String>();

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

          AuthorView author = AuthorView.builder()
              .setGivenNames(givenName)
              .setSurnames(surname)
              .setSuffix(suffix)
              .setOnBehalfOf(onBehalfOf)
              .setEqualContrib(equalContrib)
              .setDeceased(deceased)
              .setRelatedFootnote(relatedFootnote)
              .setCorresponding(corresponding)
              .setCurrentAddresses(currentAddresses)
              .setAffiliations(affiliations)
              .setCustomFootnotes(otherFootnotes)
              .build();

          list.add(author);
        }
      }
    } catch (Exception e) {
      //TODO: Why does this die silently?
      log.error("Error occurred while gathering the author affiliations.", e);
    }

    return list;
  }

  /**
   * Grab all affiliations and put them into their own map
   *
   * @param doc   the article XML document
   * @param xpath XPathExtractor to use to process xpath expressions
   * @return a Map of affiliate IDs and values
   */
  public static Map<String, String> getAffiliateMap(Document doc, XPathExtractor xpath) throws XPathException {
    Map<String, String> affiliateMap = new LinkedHashMap<String, String>();

    NodeList affiliationNodeList = xpath.selectNodes(doc, "//aff");

    //Map all affiliation id's to their affiliation strings
    for (int a = 0; a < affiliationNodeList.getLength(); a++) {
      Node node = affiliationNodeList.item(a);
      // Not all <aff>'s have the 'id' attribute.
      String id = (node.getAttributes().getNamedItem("id") == null) ? "" :
          node.getAttributes().getNamedItem("id").getTextContent();

      log.debug("Found affiliation node:" + id);

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
   * @param xpath XPathExtractor to use to process xpath expressions
   * @return a Map of address IDs and values
   */
  private static Map<String, String> getAddressMap(Document doc, XPathExtractor xpath) throws XPathException {
    Map<String, String> addressMap = new HashMap<String, String>();

    //Grab all the Current address information and place them into a map
    NodeList currentAddressNodeList = xpath.selectNodes(doc, "//fn[@fn-type='current-aff']");
    for (int a = 0; a < currentAddressNodeList.getLength(); a++) {
      Node node = currentAddressNodeList.item(a);
      String id = (node.getAttributes().getNamedItem("id") == null) ? "" :
          node.getAttributes().getNamedItem("id").getTextContent();

      log.debug("Current address node:" + id);

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
   * @param xpath XPathExtractor to use to process xpath expressions
   * @return a Map of footnote IDs and values
   */
  private static Map<String, String> getOtherFootnotesMap(Document doc, XPathExtractor xpath)
      throws XPathException, TransformerException {
    Map<String, String> otherFootnotesMap = new HashMap<String, String>();

    //Grab all 'other' footnotes and put them into their own map
    NodeList footnoteNodeList = xpath.selectNodes(doc, "//fn[@fn-type='other']");

    for (int a = 0; a < footnoteNodeList.getLength(); a++) {
      Node node = footnoteNodeList.item(a);
      // Not all <aff>'s have the 'id' attribute.
      String id = (node.getAttributes().getNamedItem("id") == null) ? "" :
          node.getAttributes().getNamedItem("id").getTextContent();

      log.debug("Found footnote node:" + id);

      DocumentFragment df = doc.createDocumentFragment();
      df.appendChild(node);

      String footnote = TextUtils.getAsXMLString(xpath.selectNode(df, "//p"));
      otherFootnotesMap.put(id, footnote);
    }

    return otherFootnotesMap;
  }

  /**
   * Reformat html embedded into the XML into something more easily styled on the front end
   *
   * @param source      html fragment
   * @param prependHTML if true, append a html snippet for a 'pilcro'
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
    String destination;

    destination = source.replaceAll("Current\\s[Aa]ddress:\\s*", "");

    return destination;
  }

  /**
   * Check to see if the current footnote is referenced by multiple contribs If the current footnote is also referenced
   * by another contrib We want to notify the end user of the relation
   *
   * @param doc            the document
   * @param xpathExtractor XPathExtractor to use to process xpath expressions
   * @param rid            the rid to search for, the RID is an attribute of a footnote that attaches a footnote to one
   *                       or many authors
   * @return true if the rid is referenced by contribs more then once
   * <p/>
   * * @throws XPathExpressionException
   */
  private static boolean hasRelatedFootnote(Node doc, XPathExtractor xpathExtractor, String rid)
      throws XPathException {
    String xpath = "//contrib/xref[@ref-type='fn' and @rid='" + rid + "']";

    log.debug("xpath: {}", xpath);
    NodeList nl = xpathExtractor.selectNodes(doc, xpath);
    log.debug("nodecount: {}", nl.getLength());

    if (nl.getLength() > 1) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Kludge for FEND-794, A better ways of doing this?
   * <p/>
   * Reformat html embedded into the XML into something more easily styled on the front end
   *
   * @param source html fragment
   * @return html fragment
   */
  public static String transFormCorresponding(String source) {
    return MARKUP_REPLACER.replace(source);
  }
}
