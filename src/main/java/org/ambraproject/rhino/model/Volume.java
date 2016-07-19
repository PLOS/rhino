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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
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
  private Long volumeId;

  @Column
  private String doi;

  @Column
  private String displayName;

  @OneToOne
  @JoinColumn(name = "imageArticleId")
  private ArticleTable imageArticle;

  @Column
  private Date created;

  @Column
  private Date lastModified;

  @Cascade(CascadeType.SAVE_UPDATE)
  @OneToMany(fetch = FetchType.LAZY, orphanRemoval = true)
  @JoinTable(
      name = "issue",
      joinColumns = @JoinColumn(name = "volumeId"),
      inverseJoinColumns = @JoinColumn(name = "issueId"))
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

    if (volumeId != null ? !volumeId.equals(volume.volumeId) : volume.volumeId != null)
      return false;
    if (doi != null ? !doi.equals(volume.doi) : volume.doi != null) return false;
    if (displayName != null ? !displayName.equals(volume.displayName) : volume.displayName != null)
      return false;
    if (imageArticle != null ? !imageArticle.equals(volume.imageArticle) : volume.imageArticle != null)
      return false;
    if (created != null ? !created.equals(volume.created) : volume.created != null) return false;
    if (lastModified != null ? !lastModified.equals(volume.lastModified) : volume.lastModified != null)
      return false;
    return issues != null ? issues.equals(volume.issues) : volume.issues == null;

  }

  @Override
  public int hashCode() {
    int result = volumeId != null ? volumeId.hashCode() : 0;
    result = 31 * result + (doi != null ? doi.hashCode() : 0);
    result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
    result = 31 * result + (imageArticle != null ? imageArticle.hashCode() : 0);
    result = 31 * result + (created != null ? created.hashCode() : 0);
    result = 31 * result + (lastModified != null ? lastModified.hashCode() : 0);
    result = 31 * result + (issues != null ? issues.hashCode() : 0);
    return result;
  }
}
