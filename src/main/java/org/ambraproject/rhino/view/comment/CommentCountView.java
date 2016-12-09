package org.ambraproject.rhino.view.comment;

public class CommentCountView {

  private final long all;
  private final long root;
  private final long removed;

  public CommentCountView(long all, long root, long removed) {
    this.all = all;
    this.root = root;
    this.removed = removed;
  }

  public long getAll() {
    return all;
  }

  public long getRoot() {
    return root;
  }

  public long getRemoved() {
    return removed;
  }

}
