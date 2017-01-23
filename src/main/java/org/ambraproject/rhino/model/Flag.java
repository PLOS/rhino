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
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

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
 * @author Alex Kudlick 3/7/12
 */
@Entity
@Table(name = "commentFlag")
public class Flag implements Timestamped {

  @Id
  @GeneratedValue
  @Column
  private Long commentFlagId;

  @Column
  private String comment;

  @Column
  private Long userProfileId;

  @Column
  @Type(type = "org.ambraproject.rhino.config.HibernateAdaptingType",
      parameters = {@Parameter(name = "class", value = "org.ambraproject.rhino.model.FlagReasonCode"),
          @Parameter(name = "adapter", value = "ADAPTER")})
  private FlagReasonCode reason;

  @ManyToOne
  @JoinColumn(name = "commentId", nullable = false)
  private Comment flaggedComment;

  @Generated(value= GenerationTime.INSERT)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(insertable=false, updatable=false, columnDefinition="timestamp default current_timestamp")
  private Date created;

  @Generated(value= GenerationTime.ALWAYS)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(insertable=false, updatable=false, columnDefinition="timestamp default current_timestamp")
  private Date lastModified;

  public Flag() {
    super();
    this.reason = FlagReasonCode.OTHER;
  }

  public Flag(Long userProfileId, FlagReasonCode reason, Comment flaggedComment) {
    super();
    this.userProfileId = userProfileId;
    this.reason = reason;
    this.flaggedComment = flaggedComment;
  }

  public Long getCommentFlagId() {
    return commentFlagId;
  }

  public void setCommentFlagId(Long commentId) {
    this.commentFlagId = commentId;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Long getUserProfileId() {
    return userProfileId;
  }

  public void setUserProfileId(Long userProfileId) {
    this.userProfileId = userProfileId;
  }

  public FlagReasonCode getReason() {
    return reason;
  }

  public void setReason(FlagReasonCode reason) {
    this.reason = reason;
  }

  public Comment getFlaggedComment() {
    return flaggedComment;
  }

  public void setFlaggedComment(Comment flaggedComment) {
    this.flaggedComment = flaggedComment;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  @Override
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

    Flag flag = (Flag) o;

    if (commentFlagId != flag.commentFlagId) return false;
    if (comment != null ? !comment.equals(flag.comment) : flag.comment != null) return false;
    if (userProfileId != null ? !userProfileId.equals(flag.userProfileId) : flag.userProfileId != null)
      return false;
    if (reason != flag.reason) return false;
    return flaggedComment != null ? flaggedComment.equals(flag.flaggedComment) : flag.flaggedComment == null;

  }

  @Override
  public int hashCode() {
    int result = Long.hashCode(commentFlagId);
    result = 31 * result + (comment != null ? comment.hashCode() : 0);
    result = 31 * result + (userProfileId != null ? userProfileId.hashCode() : 0);
    result = 31 * result + (reason != null ? reason.hashCode() : 0);
    result = 31 * result + (flaggedComment != null ? flaggedComment.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Flag{" +
        "comment='" + comment + '\'' +
        ", reason=" + reason +
        '}';
  }
}
