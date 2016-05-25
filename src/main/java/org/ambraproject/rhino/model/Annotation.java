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

/**
 * @author Alex Kudlick 3/7/12
 */
public class Annotation extends AmbraEntity {

  private AnnotationType type;
  private Long userProfileID;

  //ID of the article to which this refers
  private Long articleID;

  //If this is a reply, this holds the ID of the annotation which this is in reply to
  private Long parentID;
  private String annotationUri;

  private String title;
  private String body;
  private String highlightedText;

  private String competingInterestBody;

  private boolean isRemoved;

  public Annotation() {
    super();
  }

  public Annotation(Long userProfileID, AnnotationType type, Long articleID) {
    this();

    this.userProfileID = userProfileID;
    this.type = type;
    this.articleID = articleID;
  }

  public Long getUserProfileID() {
    return userProfileID;
  }

  public void setUserProfileID(Long userProfileID) {
    this.userProfileID = userProfileID;
  }

  public Long getArticleID() {
    return articleID;
  }

  public void setArticleID(Long articleID) {
    this.articleID = articleID;
  }

  public Long getParentID() {
    return parentID;
  }

  public void setParentID(Long parentID) {
    this.parentID = parentID;
  }

  public String getAnnotationUri() {
    return annotationUri;
  }

  public void setAnnotationUri(String annotationUri) {
    this.annotationUri = annotationUri;
  }

  public AnnotationType getType() {
    return type;
  }

  public void setType(AnnotationType type) {
    this.type = type;
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
    if (!(o instanceof Annotation)) return false;

    Annotation that = (Annotation) o;

    if (articleID != null ? !articleID.equals(that.articleID) : that.articleID != null) return false;
    if (body != null ? !body.equals(that.body) : that.body != null) return false;
    if (title != null ? !title.equals(that.title) : that.title != null) return false;
    if (type != that.type) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = articleID != null ? articleID.hashCode() : 0;
    result = 31 * result + (type != null ? type.hashCode() : 0);
    result = 31 * result + (title != null ? title.hashCode() : 0);
    result = 31 * result + (body != null ? body.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Annotation{" +
        "type=" + type +
        ", title='" + title + '\'' +
        ", body='" + body + '\'' +
        '}';
  }
}
