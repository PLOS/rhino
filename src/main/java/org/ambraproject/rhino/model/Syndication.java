package org.ambraproject.rhino.model;

import java.util.Date;

/**
 * Represents data about the status of a syndication for a single article to a single target (e.g. the status of an
 * article being sent to PMC)
 *
 * @author Alex Kudlick  11/17/11
 */
public class Syndication extends AmbraEntity {
  /**
   * This Article has been published, but has not yet been submitted to this syndication target.
   */
  public static final String STATUS_PENDING = "PENDING";
  /**
   * This Article has been submitted to this syndication target, but the process is not yet complete.
   */
  public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
  /**
   * This Article has been successfully submitted to this syndication target.
   */
  public static final String STATUS_SUCCESS = "SUCCESS";
  /**
   * This Article was submitted to this syndication target, but the process failed. The reason for this failure should
   * be written into the <i>errorMessage</i> variable.
   */
  public static final String STATUS_FAILURE = "FAILURE";


  private String doi;
  private String target;
  private String status;
  private int submissionCount;
  private String errorMessage;
  private Date lastSubmitTimestamp;

  public Syndication() {
    super();
  }

  public Syndication(String doi, String target) {
    this();
    this.target = target;
    this.doi = doi;
  }

  public String getDoi() {
    return doi;
  }

  public void setDoi(String doi) {
    this.doi = doi;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public int getSubmissionCount() {
    return submissionCount;
  }

  public void setSubmissionCount(int submissionCount) {
    this.submissionCount = submissionCount;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public Date getLastSubmitTimestamp() {
    return lastSubmitTimestamp;
  }

  public void setLastSubmitTimestamp(Date lastSubmitTimestamp) {
    this.lastSubmitTimestamp = lastSubmitTimestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Syndication)) return false;

    Syndication that = (Syndication) o;

    if (doi != null ? !doi.equals(that.doi) : that.doi != null) return false;
    if (status != null ? !status.equals(that.status) : that.status != null) return false;
    if (target != null ? !target.equals(that.target) : that.target != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = doi != null ? doi.hashCode() : 0;
    result = 31 * result + (target != null ? target.hashCode() : 0);
    result = 31 * result + (status != null ? status.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Syndication{" +
        "doi='" + doi + '\'' +
        ", target='" + target + '\'' +
        ", status='" + status + '\'' +
        '}';
  }
}
