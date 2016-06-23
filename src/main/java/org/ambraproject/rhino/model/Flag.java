/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2012 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.model;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
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
  @JoinColumn(name = "commentId")
  private Comment flaggedComment;

  @Column
  private Date created;

  @Column
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
