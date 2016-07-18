package org.ambraproject.rhino.view.article.versioned;

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.identity.Doi;

import java.util.List;
import java.util.Objects;

public class RelationshipSetView {

  private final ImmutableList<RelationshipView> inbound;
  private final ImmutableList<RelationshipView> outbound;
  private final ImmutableList<RelationshipView> declared;

  public RelationshipSetView(List<RelationshipView> inbound,
                             List<RelationshipView> outbound,
                             List<RelationshipView> declared) {
    this.inbound = ImmutableList.copyOf(inbound);
    this.outbound = ImmutableList.copyOf(outbound);
    this.declared = ImmutableList.copyOf(declared);
  }

  public static class RelationshipView {
    private final String type;
    private final String doi;
    private final String title; // nullable

    public RelationshipView(String type, Doi doi, String title) {
      this.type = Objects.requireNonNull(type);
      this.doi = doi.getName();
      this.title = title;
    }
  }

}
