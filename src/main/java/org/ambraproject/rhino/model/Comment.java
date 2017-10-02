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
  @JoinColumn(name = "articleId", nullable = false)
  private Article article;

  @ManyToOne
  @JoinColumn(name = "parentId", nullable = true)
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

  @Generated(value = GenerationTime.INSERT)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(insertable = false, updatable = false, columnDefinition = "timestamp default current_timestamp")
  private Date created;

  @Generated(value = GenerationTime.ALWAYS)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(insertable = false, updatable = false, columnDefinition = "timestamp default current_timestamp")
  private Date lastModified;

  @Column
  private boolean isRemoved;

  @Column
  private String authorEmailAddress;

  @Column
  private String authorName;

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

  public Article getArticle() {
    return article;
  }

  public void setArticle(Article article) {
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

  public String getAuthorEmailAddress() {
    return authorEmailAddress;
  }

  public void setAuthorEmailAddress(String authorEmailAddress) {
    this.authorEmailAddress = authorEmailAddress;
  }

  public String getAuthorName() {
    return authorName;
  }

  public void setAuthorName(String authorName) {
    this.authorName = authorName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Comment comment = (Comment) o;

    if (isRemoved != comment.isRemoved) return false;
    if (!commentId.equals(comment.commentId)) return false;
    if (userProfileID != null ? !userProfileID.equals(comment.userProfileID) : comment.userProfileID != null)
      return false;
    if (!article.equals(comment.article)) return false;
    if (parent != null ? !parent.equals(comment.parent) : comment.parent != null) return false;
    if (commentUri != null ? !commentUri.equals(comment.commentUri) : comment.commentUri != null)
      return false;
    if (title != null ? !title.equals(comment.title) : comment.title != null) return false;
    if (body != null ? !body.equals(comment.body) : comment.body != null) return false;
    if (highlightedText != null ? !highlightedText.equals(comment.highlightedText) : comment.highlightedText != null)
      return false;
    if (competingInterestBody != null ? !competingInterestBody.equals(comment.competingInterestBody) : comment.competingInterestBody != null)
      return false;
    if (!created.equals(comment.created)) return false;
    if (!lastModified.equals(comment.lastModified)) return false;
    if (authorEmailAddress != null ? !authorEmailAddress.equals(comment.authorEmailAddress) : comment.authorEmailAddress != null)
      return false;
    return authorName != null ? authorName.equals(comment.authorName) : comment.authorName == null;
  }

  @Override
  public int hashCode() {
    int result = commentId.hashCode();
    result = 31 * result + (userProfileID != null ? userProfileID.hashCode() : 0);
    result = 31 * result + article.hashCode();
    result = 31 * result + (parent != null ? parent.hashCode() : 0);
    result = 31 * result + (commentUri != null ? commentUri.hashCode() : 0);
    result = 31 * result + (title != null ? title.hashCode() : 0);
    result = 31 * result + (body != null ? body.hashCode() : 0);
    result = 31 * result + (highlightedText != null ? highlightedText.hashCode() : 0);
    result = 31 * result + (competingInterestBody != null ? competingInterestBody.hashCode() : 0);
    result = 31 * result + created.hashCode();
    result = 31 * result + lastModified.hashCode();
    result = 31 * result + (isRemoved ? 1 : 0);
    result = 31 * result + (authorEmailAddress != null ? authorEmailAddress.hashCode() : 0);
    result = 31 * result + (authorName != null ? authorName.hashCode() : 0);
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
