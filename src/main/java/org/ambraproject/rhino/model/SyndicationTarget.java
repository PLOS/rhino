package org.ambraproject.rhino.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.util.EnumSet;

public enum SyndicationTarget {
  PMC,
  PUBMED,
  CROSSREF,
  JISC;

  private final String label;

  private SyndicationTarget() {
    this.label = name();
  }

  /**
   * @return the string used as a persistent value
   */
  public String getLabel() {
    return label;
  }

  private static final ImmutableMap<String, SyndicationTarget> BY_LABEL = Maps.uniqueIndex(
      EnumSet.allOf(SyndicationTarget.class), SyndicationTarget::getLabel);

  public static ImmutableSet<String> getValidLabels() {
    return BY_LABEL.keySet();
  }

}
