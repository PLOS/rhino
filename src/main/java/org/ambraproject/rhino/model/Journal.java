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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
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

  @Column
  private String title;

  @JoinColumn(name = "currentIssueId")
  @ManyToOne
  private Issue currentIssue;

  @Cascade(CascadeType.SAVE_UPDATE)
  @OneToMany(fetch = FetchType.LAZY)
  @JoinColumn(name = "journalId", nullable = false)
  @OrderColumn(name="journalSortOrder")
  private List<Volume> volumes;

  @Cascade(CascadeType.SAVE_UPDATE)
  @OneToMany(fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinColumn(name = "journalId", nullable = false)
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
        ", title='" + title + '\'' +
        '}';
  }
}
