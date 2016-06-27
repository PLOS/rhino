package org.ambraproject.rhino.model;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import java.util.Date;

/**
 * Represents data about the status of a syndication for a single article to a single target (e.g. the status of an
 * article being sent to PMC)
 *
 * @author Alex Kudlick  11/17/11
 */
@Entity
@Table(name = "syndication")
public class Syndication implements Timestamped{

  @Id @GeneratedValue
  @Column
  private int syndicationId;

  @ManyToOne
  @JoinColumn(name = "versionId")
  private ArticleVersion articleVersion;

  @Column
  private String target;

  @Column
  private String status;

  @Column
  private int submissionCount;

  @Column
  private String errorMessage;

  @Column
  private Date lastSubmitTimestamp;

  @Generated(value= GenerationTime.INSERT)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(insertable=false, updatable=false, columnDefinition="timestamp default current_timestamp")
  private Date created;

  @Generated(value= GenerationTime.ALWAYS)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(insertable=false, updatable=false, columnDefinition="timestamp default current_timestamp")
  private Date lastModified;

  public Syndication() {
    super();
  }

  public Syndication(ArticleVersion articleVersion, String target) {
    this();
    this.target = target;
    this.articleVersion = articleVersion;
  }

  public int getSyndicationId() {
    return syndicationId;
  }

  public void setSyndicationId(int syndicationId) {
    this.syndicationId = syndicationId;
  }

  public ArticleVersion getArticleVersion() {
    return articleVersion;
  }

  public void setArticleVersion(ArticleVersion articleVersion) {
    this.articleVersion = articleVersion;
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

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Date getLastModified() {
    return lastModified;
  }

  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Syndication that = (Syndication) o;

    if (syndicationId != that.syndicationId) return false;
    if (submissionCount != that.submissionCount) return false;
    if (!articleVersion.equals(that.articleVersion)) return false;
    if (!target.equals(that.target)) return false;
    if (!status.equals(that.status)) return false;
    if (errorMessage != null ? !errorMessage.equals(that.errorMessage) : that.errorMessage != null)
      return false;
    return lastSubmitTimestamp != null ? lastSubmitTimestamp.equals(that.lastSubmitTimestamp) : that.lastSubmitTimestamp == null;

  }

  @Override
  public int hashCode() {
    int result = syndicationId;
    result = 31 * result + articleVersion.hashCode();
    result = 31 * result + target.hashCode();
    result = 31 * result + status.hashCode();
    result = 31 * result + submissionCount;
    result = 31 * result + (errorMessage != null ? errorMessage.hashCode() : 0);
    result = 31 * result + (lastSubmitTimestamp != null ? lastSubmitTimestamp.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Syndication{" +
        "articleVersion='" + articleVersion + '\'' +
        ", target='" + target + '\'' +
        ", status='" + status + '\'' +
        '}';
  }
}
