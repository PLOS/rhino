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
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

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
  public ArticleAsset build(ArticleAsset asset) throws XmlContentException {
    asset.setDoi(assetId.getKey());
    AssetIdentity.setNoFile(asset);

    Node contextNode = xml;
    if (GRAPHIC.equals(contextNode.getNodeName())) {
      // Ambra treats "graphic" as a special case and uses the parent node instead.
      // TODO: Ambra bug? Just using contextElement="graphic" makes more sense and is consistent with other cases.
      contextNode = contextNode.getParentNode();
    }
    asset.setContextElement(contextNode.getNodeName());

    asset.setTitle(Strings.nullToEmpty(readString("child::label")));
    Node captionNode = readNode("child::caption");
    asset.setDescription((captionNode != null) ? buildTextWithMarkup(captionNode) : "");

    return asset;
  }

}
