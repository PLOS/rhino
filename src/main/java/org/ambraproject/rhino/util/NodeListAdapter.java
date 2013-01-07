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

package org.ambraproject.rhino.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.AbstractList;
import java.util.List;

/**
 * Adapts a {@link NodeList} to be a {@link List} compatible with the Java Collections Framework.
 */
public class NodeListAdapter extends AbstractList<Node> {

  private final NodeList nodes;

  private NodeListAdapter(NodeList nodes) {
    super();
    this.nodes = Preconditions.checkNotNull(nodes);
  }

  /**
   * Wrap a node list. The returned list is unmodifiable and supports all non-destructive operations. Any changes in the
   * underlying list will be reflected in this object. (The {@link NodeList} contract implies, but does not explicitly
   * guarantee, that such changes should not be possible.)
   *
   * @param nodes a DOM node list
   * @return a Java Collections Framework node list
   * @throws NullPointerException if {@code nodes} is null
   */
  public static List<Node> wrap(NodeList nodes) {
    return new NodeListAdapter(nodes);
  }

  /**
   * Make a copy of a node list. The returned list is guaranteed not to change regardless of the original list's
   * behavior.
   *
   * @param nodes a DOM node list
   * @return an immutable Java Collections Framework node list
   * @throws NullPointerException if {@code nodes} is null
   */
  public static ImmutableList<Node> copy(NodeList nodes) {
    return ImmutableList.copyOf(wrap(nodes));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node get(int index) {
    // To match the List contract (nodes.item returns null on invalid index)
    Preconditions.checkElementIndex(index, size());

    Node node = nodes.item(index);
    if (node == null) {
      throw new NullPointerException("Delegate NodeList returned null for a valid index");
    }
    return node;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int size() {
    return nodes.getLength();
  }

}
