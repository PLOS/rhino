package org.ambraproject.rhino.content.xml;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import org.w3c.dom.Node;

/**
 * A set of article XML nodes, each representing an asset, mapped by the asset's DOI.
 */
public class AssetNodesByDoi {

  private final ImmutableListMultimap<String, Node> nodeMap;

  public AssetNodesByDoi(ListMultimap<String, Node> nodeMap) {
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
  public ImmutableSet<String> getAllDois() {
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
   *                                  !this.getAllDois().contains(doi)})
   * @throws NullPointerException     if {@code doi == null}
   */
  public ImmutableList<Node> getNodes(String doi) {
    Preconditions.checkNotNull(doi);
    ImmutableList<Node> nodes = nodeMap.get(doi);
    if (nodes.isEmpty()) {
      throw new IllegalArgumentException("DOI not matched to asset node: " + doi);
    }
    return nodes;
  }

}
