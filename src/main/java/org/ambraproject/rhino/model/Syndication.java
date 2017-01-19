/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

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
  @JoinColumn(name = "revisionId", nullable = false)
  private ArticleRevision articleRevision;

  @Column
  private String targetQueue;

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

  public Syndication(ArticleRevision articleRevision, String targetQueue) {
    this();
    this.targetQueue = targetQueue;
    this.articleRevision = articleRevision;
  }

  public int getSyndicationId() {
    return syndicationId;
  }

  public void setSyndicationId(int syndicationId) {
    this.syndicationId = syndicationId;
  }

  public ArticleRevision getArticleRevision() {
    return articleRevision;
  }

  public void setArticleRevision(ArticleRevision articleRevision) {
    this.articleRevision = articleRevision;
  }

  public String getTarget() {
    return targetQueue;
  }

  public void setTarget(String target) {
    this.targetQueue = target;
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
    if (articleRevision != null ? !articleRevision.equals(that.articleRevision) : that.articleRevision != null) {
      return false;
    }
    return targetQueue != null ? targetQueue.equals(that.targetQueue) : that.targetQueue == null;
  }

  @Override
  public int hashCode() {
    int result = articleRevision != null ? articleRevision.hashCode() : 0;
    result = 31 * result + (targetQueue != null ? targetQueue.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Syndication{" +
        "syndicationId=" + syndicationId +
        ", articleRevision=" + articleRevision +
        ", targetQueue='" + targetQueue + '\'' +
        ", status='" + status + '\'' +
        ", submissionCount=" + submissionCount +
        ", errorMessage='" + errorMessage + '\'' +
        ", lastSubmitTimestamp=" + lastSubmitTimestamp +
        ", created=" + created +
        ", lastModified=" + lastModified +
        '}';
  }
}
