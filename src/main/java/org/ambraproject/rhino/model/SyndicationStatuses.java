package org.ambraproject.rhino.model;

public enum SyndicationStatuses {
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
  FAILURE,
}
