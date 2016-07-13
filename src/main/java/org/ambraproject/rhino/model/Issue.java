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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;
import java.util.List;

/**
 * model class containing issue information
 *
 * @author Juan Peralta
 */
@Entity
@Table(name = "issue")
public class Issue implements Timestamped {

  @Id
  @GeneratedValue
  private Long issueId;

  @Column
  private String issueUri;

  @Column
  private String displayName;

  @Column
  private Boolean respectOrder;

  @Column
  private String imageUri;

  @Column
  private String title;

  @Column
  private String description;

  @Column
  private Date created;

  @Column
  private Date lastModified;

  @Transient
  private List<ArticleTable> articles;

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

  public List<ArticleTable> getArticles() {
    return articles;
  }

  public void setArticles(List<ArticleTable> articles) {
    this.articles = articles;
  }

  public Long getIssueId() {
    return issueId;
  }

  public void setIssueId(Long issueId) {
    this.issueId = issueId;
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

    Issue issue = (Issue) o;

    if (respectOrder != issue.respectOrder) return false;
    if (issueId != null ? !issueId.equals(issue.issueId) : issue.issueId != null) return false;
    if (issueUri != null ? !issueUri.equals(issue.issueUri) : issue.issueUri != null) return false;
    if (displayName != null ? !displayName.equals(issue.displayName) : issue.displayName != null)
      return false;
    if (imageUri != null ? !imageUri.equals(issue.imageUri) : issue.imageUri != null) return false;
    if (title != null ? !title.equals(issue.title) : issue.title != null) return false;
    if (description != null ? !description.equals(issue.description) : issue.description != null)
      return false;
    if (created != null ? !created.equals(issue.created) : issue.created != null) return false;
    if (lastModified != null ? !lastModified.equals(issue.lastModified) : issue.lastModified != null)
      return false;
    return articles != null ? articles.equals(issue.articles) : issue.articles == null;

  }

  @Override
  public int hashCode() {
    int result = issueId != null ? issueId.hashCode() : 0;
    result = 31 * result + (issueUri != null ? issueUri.hashCode() : 0);
    result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
    result = 31 * result + (respectOrder ? 1 : 0);
    result = 31 * result + (imageUri != null ? imageUri.hashCode() : 0);
    result = 31 * result + (title != null ? title.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (created != null ? created.hashCode() : 0);
    result = 31 * result + (lastModified != null ? lastModified.hashCode() : 0);
    result = 31 * result + (articles != null ? articles.hashCode() : 0);
    return result;
  }
}
