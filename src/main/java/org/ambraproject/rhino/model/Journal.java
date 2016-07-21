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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Model class containing all information for a journal.
 *
 * @author Juan Peralta 4/12/2012
 */
@Entity
@Table(name = "journal")
public class Journal implements Timestamped {

  @Id
  @GeneratedValue
  @Column
  private Long journalId;

  @Column
  private String journalKey;

  @Column
  private String eIssn;

  @OneToOne
  @JoinColumn(name = "imageArticleId")
  private ArticleTable imageArticle;

  @Column
  private String title;

  @JoinColumn(name = "currentIssueID")
  @ManyToOne
  private Issue currentIssue;

  @Cascade(CascadeType.SAVE_UPDATE)
  @OneToMany(fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinTable(
      name = "volume",
      joinColumns = @JoinColumn(name = "journalId"),
      inverseJoinColumns = @JoinColumn(name = "volumeID"))
  private List<Volume> volumes;

  @Cascade(CascadeType.SAVE_UPDATE)
  @OneToMany(fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinTable(
      name = "articleList",
      joinColumns = @JoinColumn(name = "journalId"),
      inverseJoinColumns = @JoinColumn(name = "articleListId"))
  private Collection<ArticleList> articleLists;

  @Generated(value= GenerationTime.INSERT)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(insertable=false, updatable=false, columnDefinition="timestamp default current_timestamp")
  private Date created;

  @Generated(value= GenerationTime.ALWAYS)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(insertable=false, updatable=false, columnDefinition="timestamp default current_timestamp")
  private Date lastModified;

  public Journal() {
    super();
  }

  public Journal(String journalKey) {
    super();
    this.journalKey = journalKey;
  }

  public Long getJournalId() {
    return journalId;
  }

  public void setJournalId(Long journalId) {
    this.journalId = journalId;
  }

  public String getJournalKey() {
    return journalKey;
  }

  public void setJournalKey(String journalKey) {
    this.journalKey = journalKey;
  }

  public String geteIssn() {
    return eIssn;
  }

  public void seteIssn(String eIssn) {
    this.eIssn = eIssn;
  }

  public ArticleTable getImageArticle() {
    return imageArticle;
  }

  public void setImageArticle(ArticleTable imageArticle) {
    this.imageArticle = imageArticle;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Issue getCurrentIssue() {
    return currentIssue;
  }

  public void setCurrentIssue(Issue currentIssue) {
    this.currentIssue = currentIssue;
  }

  public List<Volume> getVolumes() {
    return volumes;
  }

  public void setVolumes(List<Volume> volumes) {
    this.volumes = volumes;
  }

  public Collection<ArticleList> getArticleLists() {
    return articleLists;
  }

  public void setArticleLists(Collection<ArticleList> articleLists) {
    this.articleLists = articleLists;
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

    Journal journal = (Journal) o;
    return journalKey != null ? journalKey.equals(journal.journalKey) : journal.journalKey == null;
  }

  @Override
  public int hashCode() {
    return journalKey != null ? journalKey.hashCode() : 0;
  }

  @Override
  public String toString() {
    return "Journal{" +
        "id='" + journalId + '\'' +
        ", journalKey='" + journalKey + '\'' +
        ", eIssn='" + eIssn + '\'' +
        ", imageArticle='" + imageArticle + '\'' +
        ", title='" + title + '\'' +
        '}';
  }
}
