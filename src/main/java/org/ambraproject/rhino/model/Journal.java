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


import java.util.Collection;
import java.util.List;

/**
 * Model class containing all information for a journal.
 *
 * @author Juan Peralta 4/12/2012
 */
public class Journal extends AmbraEntity {

  private String journalKey;
  private String eIssn;
  private String imageUri;
  private String title;
  private String description;

  private Issue currentIssue;

  private List<Volume> volumes;

  private Collection<ArticleList> articleLists;

  public Journal() {
    super();
  }

  public Journal(String journalKey) {
    super();
    this.journalKey = journalKey;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Journal)) return false;

    Journal journal = (Journal) o;

    if (getID() != null ? !getID().equals(journal.getID()) : journal.getID() != null) return false;
    if (description != null ? !description.equals(journal.description) : journal.description != null) return false;
    if (eIssn != null ? !eIssn.equals(journal.eIssn) : journal.eIssn != null) return false;
    if (imageUri != null ? !imageUri.equals(journal.imageUri) : journal.imageUri != null) return false;
    if (journalKey != null ? !journalKey.equals(journal.journalKey) : journal.journalKey != null) return false;
    if (title != null ? !title.equals(journal.title) : journal.title != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = getID() != null ? getID().hashCode() : 0;
    result = 31 * result + (journalKey != null ? journalKey.hashCode() : 0);
    result = 31 * result + (eIssn != null ? eIssn.hashCode() : 0);
    result = 31 * result + (imageUri != null ? imageUri.hashCode() : 0);
    result = 31 * result + (title != null ? title.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Journal{" +
        "id='" + getID() + '\'' +
        ", journalKey='" + journalKey + '\'' +
        ", eIssn='" + eIssn + '\'' +
        ", imageUri='" + imageUri + '\'' +
        ", title='" + title + '\'' +
        ", description='" + description + '\'' +
        '}';
  }
}
