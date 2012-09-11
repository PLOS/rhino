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

package org.ambraproject.admin.xpath;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.ambraproject.admin.controller.DoiBasedIdentity;
import org.ambraproject.models.ArticleAsset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.List;

/**
 * Contains a whole article as an NLM-format XML document and extracts metadata for one asset.
 */
public class AssetXml extends XmlToObject<ArticleAsset> {

  private static final Logger log = LoggerFactory.getLogger(AssetXml.class);

  private final DoiBasedIdentity assetId;

  public AssetXml(Node xml, DoiBasedIdentity assetId) {
    super(xml);
    this.assetId = Preconditions.checkNotNull(assetId);
  }

  // The node-names for nodes that can be an asset, separated by where to find the DOI
  private static final ImmutableSet<String> ASSET_WITH_OBJID = ImmutableSet.of("table-wrap", "fig");
  private static final ImmutableSet<String> ASSET_WITH_HREF = ImmutableSet.of("supplementary-material", "inline-graphic");

  // An XPath expression that will match any node with one of the names above
  private static final String ASSET_EXPRESSION = String.format("//(%s)",
      Joiner.on('|').join(Iterables.concat(ASSET_WITH_OBJID, ASSET_WITH_HREF)));

  @Override
  public ArticleAsset build(ArticleAsset obj) throws XmlContentException {
    List<Node> allAssetNodes = readNodeList(ASSET_EXPRESSION);
    Node matchingAssetNode = findMatchingAsset(allAssetNodes);
    return parseAsset(matchingAssetNode, obj);
  }

  /*
   * TODO: Query directly for the correct node instead of finding all of them and iterating
   */
  private Node findMatchingAsset(List<Node> assetNodes) throws XmlContentException {
    if (assetNodes == null) {
      assetNodes = ImmutableList.of(); // skip to error
    }

    final String targetDoi = assetId.getIdentifier();
    for (Node assetNode : assetNodes) {
      String nodeName = assetNode.getNodeName();
      String doi;
      if (ASSET_WITH_OBJID.contains(nodeName)) {
        doi = readString("object-id[@pub-id-type=\"doi\"]", assetNode);
      } else if (ASSET_WITH_HREF.contains(nodeName)) {
        doi = parseAssetWithHref(assetNode);
      } else {
        String message = "Expected an asset node; received node of unrecognized type: " + nodeName;
        throw new IllegalArgumentException(message);
      }

      if (doi == null) {
        log.warn("An asset node ({}) does not have DOI as expected", nodeName);
      } else {
        if (doi.equals(targetDoi)) {
          return assetNode;
        }
      }
    }

    String errorMsg = "Article XML does not have an asset with DOI=" + targetDoi;
    throw new XmlContentException(errorMsg);
  }

  /*
   * Read the "xlink:href" attribute from a <supplementary-material> or <inline-graphic> node.
   *
   * TODO: Use XPath instead and handle the XML namespace properly.
   */
  private static String parseAssetWithHref(Node assetNode) {
    NamedNodeMap attributes = assetNode.getAttributes();
    if (attributes == null) {
      return null;
    }
    Node hrefAttr = attributes.getNamedItem("xlink:href");
    if (hrefAttr == null) {
      return null;
    }
    return hrefAttr.getTextContent();
  }

  private ArticleAsset parseAsset(Node assetNode, ArticleAsset asset) {
    asset.setDoi(assetId.getKey());
    asset.setExtension(assetId.getExtension());

    asset.setTitle(readString("caption/title", assetNode));
    asset.setTitle(readString("caption/p", assetNode)); // TODO Need to support multiple paragraphs?

    return asset;
  }

}
