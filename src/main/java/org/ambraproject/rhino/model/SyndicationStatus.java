package org.ambraproject.rhino.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.util.EnumSet;

public enum SyndicationStatus {
  /**
   * This Article has been published, but has not yet been submitted to this syndication target.
   */
  PENDING,
  /**
   * This Article has been submitted to this syndication target, but the process is not yet complete.
   */
  IN_PROGRESS,
  /**
   * This Article has been successfully submitted to this syndication target.
   */
  SUCCESS,
  /**
   * This Article was submitted to this syndication target, but the process failed. The reason for this failure should
   * be written into the <i>errorMessage</i> variable.
   */
  FAILURE;

  private final String label;

  private SyndicationStatus() {
    this.label = name();
  }

  /**
   * @return the string used as a persistent value
   */
  public String getLabel() {
    return label;
  }

  private static final ImmutableMap<String, SyndicationStatus> BY_LABEL = Maps.uniqueIndex(
      EnumSet.allOf(SyndicationStatus.class), SyndicationStatus::getLabel);

  public static ImmutableSet<String> getValidLabels() {
    return BY_LABEL.keySet();
  }

}
