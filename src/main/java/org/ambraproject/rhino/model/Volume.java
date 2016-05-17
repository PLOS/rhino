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
 * model class containing volume information
 *
 * @author Juan Peralta
 */
public class Volume extends AmbraEntity {
  private String volumeUri;
  private String displayName;
  private String imageUri;
  private String title;
  private String description;

  private List<Issue> issues;

  public Volume() {
    super();
  }

  public Volume(String volumeUri) {
    super();
    this.volumeUri = volumeUri;
  }

  public String getVolumeUri() {
    return volumeUri;
  }

  public void setVolumeUri(String volumeUri) {
    this.volumeUri = volumeUri;
  }

  public List<Issue> getIssues() {
    return issues;
  }

  public void setIssues(List<Issue> issues) {
    this.issues = issues;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Volume)) return false;

    Volume volume = (Volume) o;

    if (getID() != null ? !getID().equals(volume.getID()) : volume.getID() != null) return false;
    if (description != null ? !description.equals(volume.description) : volume.description != null) return false;
    if (displayName != null ? !displayName.equals(volume.displayName) : volume.displayName != null) return false;
    if (imageUri != null ? !imageUri.equals(volume.imageUri) : volume.imageUri != null) return false;
    if (title != null ? !title.equals(volume.title) : volume.title != null) return false;
    if (volumeUri != null ? !volumeUri.equals(volume.volumeUri) : volume.volumeUri != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = getID() != null ? getID().hashCode() : 0;
    result = 31 * result + (volumeUri != null ? volumeUri.hashCode() : 0);
    result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
    result = 31 * result + (imageUri != null ? imageUri.hashCode() : 0);
    result = 31 * result + (title != null ? title.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Volume{" +
        "id='" + getID() + '\'' +
        ", volumeUri='" + volumeUri + '\'' +
        ", displayName='" + displayName + '\'' +
        ", imageUri='" + imageUri + '\'' +
        ", title='" + title + '\'' +
        ", description='" + description + '\'' +
        '}';
  }
}
