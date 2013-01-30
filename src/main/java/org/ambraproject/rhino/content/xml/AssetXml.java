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
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.util.List;

/**
 * Contains a whole article as an NLM-format XML document and extracts metadata for one asset.
 */
public class AssetXml extends AbstractArticleXml<ArticleAsset> {

  private static final Logger log = LoggerFactory.getLogger(AssetXml.class);

  private final AssetIdentity assetId;

  /**
   * The Node passed to this constructor may be a full document or just the asset node. In the former case, this class
   * will search the document for the identified asset node. The latter case is more efficient.
   *
   * @param xml
   * @param assetId
   */
  public AssetXml(Node xml, AssetIdentity assetId) {
    super(xml);
    this.assetId = Preconditions.checkNotNull(assetId);
  }

  @Override
  public ArticleAsset build(ArticleAsset obj) throws XmlContentException {
    List<AssetNode> allAssetNodes = findAllAssetNodes();
    Node matchingAssetNode = findMatchingAsset(allAssetNodes);
    return parseAsset(matchingAssetNode, obj);
  }

  /*
   * TODO: Query directly for the correct node instead of finding all of them and iterating
   */
  private Node findMatchingAsset(List<AssetNode> assetNodes) throws XmlContentException {
    final String targetDoi = assetId.getIdentifier();
    for (AssetNode assetNode : assetNodes) {
      if (targetDoi.equals(assetNode.getDoi())) {
        return assetNode.getNode();
      }
    }

    String errorMsg = "Article XML does not have an asset with DOI=" + targetDoi;
    throw new XmlContentException(errorMsg);
  }

  private ArticleAsset parseAsset(Node assetNode, ArticleAsset asset) {
    asset.setDoi(assetId.getKey());
    AssetIdentity.setNoFile(asset);

    asset.setTitle(readString("caption/title", assetNode));
    asset.setTitle(readString("caption/p", assetNode)); // TODO Need to support multiple paragraphs?

    return asset;
  }

}
