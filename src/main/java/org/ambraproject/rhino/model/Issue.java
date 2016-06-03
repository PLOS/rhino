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


import java.util.List;

/**
 * model class containing issue information
 *
 * @author Juan Peralta
 */
public class Issue extends AmbraEntity {

  private String issueUri;
  private String displayName;
  private boolean respectOrder;
  private String imageUri;
  private String title;
  private String description;

  private List<String> articleDois;

  public Issue() {
    super();
  }

  public Issue(String issueUri) {
    super();
    this.issueUri = issueUri;
  }

  public String getIssueUri() {
    return issueUri;
  }

  public void setIssueUri(String issueUri) {
    this.issueUri = issueUri;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public boolean isRespectOrder() {
    return respectOrder;
  }

  public void setRespectOrder(boolean respectOrder) {
    this.respectOrder = respectOrder;
  }

  public String getImageUri() {
    return imageUri;
  }

  public void setImageUri(String imageUri) {
    this.imageUri = imageUri;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public List<String> getArticleDois() {
    return articleDois;
  }

  public void setArticleDois(List<String> articleDois) {
    this.articleDois = articleDois;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Issue)) return false;

    Issue issue = (Issue) o;

    if (getID() != null ? !getID().equals(issue.getID()) : issue.getID() != null) return false;
    if (respectOrder != issue.respectOrder) return false;
    if (articleDois != null ? !articleDois.equals(issue.articleDois) : issue.articleDois != null) return false;
    if (description != null ? !description.equals(issue.description) : issue.description != null) return false;
    if (displayName != null ? !displayName.equals(issue.displayName) : issue.displayName != null) return false;
    if (imageUri != null ? !imageUri.equals(issue.imageUri) : issue.imageUri != null) return false;
    if (issueUri != null ? !issueUri.equals(issue.issueUri) : issue.issueUri != null) return false;
    if (title != null ? !title.equals(issue.title) : issue.title != null) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = getID() != null ? getID().hashCode() : 0;
    result = 31 * result + (issueUri != null ? issueUri.hashCode() : 0);
    result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
    result = 31 * result + (respectOrder ? 1 : 0);
    result = 31 * result + (imageUri != null ? imageUri.hashCode() : 0);
    result = 31 * result + (title != null ? title.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (articleDois != null ? articleDois.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Issue{" +
        "id='" + getID() + '\'' +
        ", issueUri='" + issueUri + '\'' +
        ", displayName='" + displayName + '\'' +
        ", respectOrder=" + respectOrder +
        ", imageUri='" + imageUri + '\'' +
        ", title='" + title + '\'' +
        ", description='" + description + '\'' +
        ", articleDois=" + articleDois +
        '}';
  }
}
