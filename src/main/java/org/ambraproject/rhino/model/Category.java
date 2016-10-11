/*
 * Copyright (c) 2007-2014 by Public Library of Science
 *
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ambraproject.rhino.model;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import java.io.Serializable;
import java.util.Date;

/**
 * @author Alex Kudlick 11/10/11
 */
@Entity
@Table(name = "category")
public class Category implements Timestamped, Serializable {

  @Id
  @GeneratedValue
  @Column
  private int categoryId;

  @Column
  private String path;

  @Generated(value= GenerationTime.INSERT)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(insertable=false, updatable=false, columnDefinition="timestamp default current_timestamp")
  private Date created;

  @Generated(value= GenerationTime.ALWAYS)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(insertable=false, updatable=false, columnDefinition="timestamp default current_timestamp")
  private Date lastModified;

  /**
   * @return the full path, starting at the top-level of the taxonomic hierarchy, to this category.  Levels are
   * delimited by slash characters. Example: "/Biology and life sciences/Cell biology/Cellular types/Animal cells/Blood
   * cells/White blood cells/T cells"
   */
  public String getPath() {
    return path;
  }

  /**
   * Sets the full path from the beginning of the taxonomic hierarchy. Levels should be delimited by forward slashes, as
   * in a Unix file path.
   *
   * @param path full path to the category
   */
  public void setPath(String path) {
    if (path == null || path.isEmpty() || path.charAt(0) != '/') {
      path = path == null ? "null" : path;
      throw new IllegalArgumentException("Invalid category path: " + path);
    }
    this.path = path;
  }

  public int getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(int categoryId) {
    this.categoryId = categoryId;
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
    if (!(o instanceof Category)) return false;

    Category other = (Category) o;
    if (path == null) {
      //We have to use the getPath() method instead of the private property as hibernate doesn't seem to populate the
      //property prior to the method call all the time.  It's some kind of proxy object magic.
      return other.getPath() == null;
    } else {
      //We have to use the getPath() method instead of the private property as hibernate doesn't seem to populate the
      //property prior to the method call all the time.  It's some kind of proxy object magic.
      return path.equals(other.getPath());
    }
  }

  @Override
  public int hashCode() {
    int result = path != null ? path.hashCode() : 0;
    return result;
  }

  @Override
  public String toString() {
    return path;
  }
}
