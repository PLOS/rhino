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

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
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
  @Column
  private Long issueId;

  @Column
  private String doi;

  @Column
  private String displayName;

  @OneToOne
  @JoinColumn(name = "imageArticleId", nullable = false)
  private ArticleTable imageArticle;

  @Generated(value= GenerationTime.INSERT)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(insertable=false, updatable=false, columnDefinition="timestamp default current_timestamp")
  private Date created;

  @Generated(value= GenerationTime.ALWAYS)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(insertable=false, updatable=false, columnDefinition="timestamp default current_timestamp")
  private Date lastModified;

  @Cascade(CascadeType.SAVE_UPDATE)
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "issueArticleList",
      joinColumns = @JoinColumn(name = "issueId", nullable = false),
      inverseJoinColumns = @JoinColumn(name = "articleId", nullable = false)
  )
  @OrderColumn(name="sortOrder", nullable=false)
  private List<ArticleTable> articles;

  public Issue() {
    super();
  }

  public Issue(String doi) {
    super();
    this.doi = doi;
  }

  public Long getIssueId() {
    return issueId;
  }

  public void setIssueId(Long issueId) {
    this.issueId = issueId;
  }

  public String getDoi() {
    return doi;
  }

  public void setDoi(String doi) {
    this.doi = doi;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public ArticleTable getImageArticle() {
    return imageArticle;
  }

  public void setImageArticle(ArticleTable imageArticle) {
    this.imageArticle = imageArticle;
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

  public List<ArticleTable> getArticles() {
    return articles;
  }

  public void setArticles(List<ArticleTable> articles) {
    this.articles = articles;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Issue issue = (Issue) o;
    return doi != null ? doi.equals(issue.doi) : issue.doi == null;
  }

  @Override
  public int hashCode() {
    return doi != null ? doi.hashCode() : 0;
  }
}
