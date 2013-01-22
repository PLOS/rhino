package org.ambraproject.rhino.content.xml;

import com.google.common.base.Preconditions;
import org.w3c.dom.Node;

/**
 * An XML node that, because of its name, is assumed to be associated with an article asset, stored together with its
 * DOI.
 */
public class AssetNode {
  private final Node node;
  private final String doi;

  AssetNode(Node node, String doi) {
    this.node = Preconditions.checkNotNull(node);
    this.doi = doi;
  }

  public String getDoi() {
    return doi;
  }

  public Node getNode() {
    return node;
  }

  @Override
  public String toString() {
    return (doi != null) ? doi : node.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AssetNode assetNode = (AssetNode) o;

    if (doi != null ? !doi.equals(assetNode.doi) : assetNode.doi != null) return false;
    if (!node.equals(assetNode.node)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = node.hashCode();
    result = 31 * result + (doi != null ? doi.hashCode() : 0);
    return result;
  }

}
