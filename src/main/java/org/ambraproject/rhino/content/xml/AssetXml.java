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

package org.ambraproject.rhino.content.xml;

import com.google.common.base.Strings;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.article.AssetMetadata;
import org.w3c.dom.Node;

import java.util.Objects;

/**
 * Contains a whole article as an NLM-format XML document and extracts metadata for one asset.
 */
public class AssetXml extends AbstractArticleXml<AssetMetadata> {
  private final Doi assetId;

  /**
   * The Node passed to this constructor may be a full document or just the asset node. In the former case, this class
   * will search the document for the identified asset node. The latter case is more efficient.
   *
   * @param xml
   * @param assetId
   */
  public AssetXml(Node xml, Doi assetId) {
    super(xml);
    this.assetId = Objects.requireNonNull(assetId);
  }

  @Override
  public AssetMetadata build() throws XmlContentException {
    String doi = assetId.getName();
    String title = "";
    String description = "";

    if (xml.getLocalName().equalsIgnoreCase(DECISION_LETTER)) {
      title = Strings.nullToEmpty(readString("front-stub/title-group/article-title"));
      description = Strings.nullToEmpty(readString("@article-type"));
    } else {
      title = Strings.nullToEmpty(readString("child::label"));
      Node captionNode = readNode("child::caption");
      description = Strings.nullToEmpty(getXmlFromNode(captionNode));
    }

    return new AssetMetadata(doi, title, description);
  }

}
