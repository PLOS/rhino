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
 * @author Alex Kudlick 3/7/12
 */
@Entity
@Table(name = "comment")
public class Comment {

  @Id
  @GeneratedValue
  @Column
  private Long commentId;

  @Column
  private Long userProfileID;

  @ManyToOne
  @JoinColumn(name = "articleId")
  private ArticleTable article;

  @ManyToOne
  @JoinColumn(name = "parentId")
  private Comment parent;

  @Column
  private String commentUri;

  @Column
  private String title;

  @Column
  private String body;

  @Column
  private String highlightedText;

  @Column
  private String competingInterestBody;

  @Generated(value= GenerationTime.INSERT)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(insertable=false, updatable=false, columnDefinition="timestamp default current_timestamp")
  private Date created;

  @Generated(value= GenerationTime.ALWAYS)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(insertable=false, updatable=false, columnDefinition="timestamp default current_timestamp")
  private Date lastModified;
  
  @Column
  private boolean isRemoved;

  public Comment() {
    super();
  }

  public Long getCommentId() {
    return commentId;
  }

  public void setCommentId(Long commentId) {
    this.commentId = commentId;
  }

  public Long getUserProfileID() {
    return userProfileID;
  }

  public void setUserProfileID(Long userProfileID) {
    this.userProfileID = userProfileID;
  }

  public ArticleTable getArticle() {
    return article;
  }

  public void setArticle(ArticleTable article) {
    this.article = article;
  }

  public Comment getParent() {
    return parent;
  }

  public Long getParentId() {
    return parent.getCommentId();
  }

  public void setParent(Comment parent) {
    this.parent = parent;
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

  public String getCommentUri() {
    return commentUri;
  }

  public void setCommentUri(String commentUri) {
    this.commentUri = commentUri;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getCompetingInterestBody() {
    return competingInterestBody;
  }

  public void setCompetingInterestBody(String competingInterestBody) {
    this.competingInterestBody = competingInterestBody;
  }

  public String getHighlightedText() {
    return highlightedText;
  }

  public void setHighlightedText(String highlightedText) {
    this.highlightedText = highlightedText;
  }

  public boolean getIsRemoved() {
    return isRemoved;
  }

  public void setIsRemoved(boolean removed) {
    isRemoved = removed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Comment)) return false;

    Comment that = (Comment) o;

    if (article != null ? !article.equals(that.article) : that.article != null) return false;
    if (body != null ? !body.equals(that.body) : that.body != null) return false;
    if (title != null ? !title.equals(that.title) : that.title != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = article != null ? article.hashCode() : 0;
    result = 31 * result + (title != null ? title.hashCode() : 0);
    result = 31 * result + (body != null ? body.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Comment{" +
        "title='" + title + '\'' +
        ", body='" + body + '\'' +
        '}';
  }
}
