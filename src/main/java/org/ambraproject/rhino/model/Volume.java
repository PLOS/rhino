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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import java.util.Date;
import java.util.List;

/**
 * model class containing volume information
 *
 * @author Juan Peralta
 */
@Entity
@Table(name = "volume")
public class Volume implements Timestamped {

  @Id
  @GeneratedValue
  @Column
  private Long volumeId;

  @Column
  private String doi;

  @Column
  private String displayName;

  @OneToOne
  @JoinColumn(name = "imageArticleId")
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
  @OneToMany(fetch = FetchType.LAZY)
  @JoinColumn(name = "volumeId")
  @OrderColumn(name="volumeSortOrder")
  private List<Issue> issues;

  public Volume() {
    super();
  }

  public Volume(String doi) {
    super();
    this.doi = doi;
  }

  public Long getVolumeId() {
    return volumeId;
  }

  public void setVolumeId(Long volumeId) {
    this.volumeId = volumeId;
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

  public List<Issue> getIssues() {
    return issues;
  }

  public void setIssues(List<Issue> issues) {
    this.issues = issues;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Volume volume = (Volume) o;
    return doi != null ? doi.equals(volume.doi) : volume.doi == null;
  }

  @Override
  public int hashCode() {
    return doi != null ? doi.hashCode() : 0;
  }
}
