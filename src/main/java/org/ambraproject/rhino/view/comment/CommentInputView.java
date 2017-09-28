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

package org.ambraproject.rhino.view.comment;

public class CommentInputView {

  private String annotationUri;
  private String creatorUserId;
  private String parentCommentId;
  private String title;
  private String body;
  private String highlightedText;
  private String competingInterestStatement;
  private String isRemoved;
  private String authorEmailAddress;
  private String authorName;

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public CommentInputView() {
  }

  public String getCreatorUserId() {
    return creatorUserId;
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setCreatorUserId(String creatorUserId) {
    this.creatorUserId = creatorUserId;
  }

  public String getParentCommentId() {
    return parentCommentId;
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setParentCommentId(String parentCommentId) {
    this.parentCommentId = parentCommentId;
  }

  public String getTitle() {
    return title;
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setTitle(String title) {
    this.title = title;
  }

  public String getBody() {
    return body;
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setBody(String body) {
    this.body = body;
  }

  public String getHighlightedText() {
    return highlightedText;
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setHighlightedText(String highlightedText) {
    this.highlightedText = highlightedText;
  }

  public String getCompetingInterestStatement() {
    return competingInterestStatement;
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setCompetingInterestStatement(String competingInterestStatement) {
    this.competingInterestStatement = competingInterestStatement;
  }

  public String getIsRemoved() {
    return isRemoved;
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setIsRemoved(String isRemoved) {
    this.isRemoved = isRemoved;
  }

  public String getAnnotationUri() {
    return annotationUri;
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setAnnotationUri(String annotationUri) {
    this.annotationUri = annotationUri;
  }

  public String getAuthorEmailAddress() {
    return authorEmailAddress;
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setAuthorEmailAddress(String authorEmailAddress) {
    this.authorEmailAddress = authorEmailAddress;
  }

  public String getAuthorName() {
    return authorName;
  }

  /**
   * @deprecated For deserializer only
   */
  @Deprecated
  public void setAuthorName(String authorName) {
    this.authorName = authorName;
  }
}
