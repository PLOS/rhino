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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import org.ambraproject.rhino.identity.Doi;
import org.w3c.dom.Node;

/**
 * A set of article XML nodes, each representing an asset, mapped by the asset's DOI.
 */
public class AssetNodesByDoi {

  private final ImmutableListMultimap<Doi, Node> nodeMap;

  AssetNodesByDoi(ListMultimap<Doi, Node> nodeMap) {
    this.nodeMap = ImmutableListMultimap.copyOf(nodeMap);
  }

  /**
   * Get the number of XML nodes contained in this object.
   *
   * @return the number of XML nodes
   */
  public int getNodeCount() {
    return nodeMap.size();
  }

  /**
   * Get the set of all asset DOIs contained in this object. The returned set preserves the order that they appear in
   * the original document.
   *
   * @return the asset DOIs
   */
  public ImmutableSet<Doi> getDois() {
    return nodeMap.keySet();
  }

  /**
   * Get the XML nodes associated with a DOI. Typically the returned list will contain exactly one node. It is valid
   * (but rare) for an article XML document to contain multiple asset nodes with the same DOI, in which case the
   * returned list contains them in their order from the original document.
   *
   * @param doi an asset DOI
   * @return a non-empty list
   * @throws IllegalArgumentException if there is no asset matching the DOI (equivalently, if {@code
   *                                  !this.getDois().contains(doi)})
   * @throws NullPointerException     if {@code doi == null}
   */
  public ImmutableList<Node> getNodes(Doi doi) {
    Preconditions.checkNotNull(doi);
    ImmutableList<Node> nodes = nodeMap.get(doi);
    if (nodes.isEmpty()) {
      throw new IllegalArgumentException("DOI not matched to asset node: " + doi);
    }
    return nodes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AssetNodesByDoi that = (AssetNodesByDoi) o;
    return nodeMap.equals(that.nodeMap);
  }

  @Override
  public int hashCode() {
    return nodeMap.hashCode();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + nodeMap.toString();
  }

}
