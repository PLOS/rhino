/*
 * $HeadURL$
 * $Id$
 *
 * Copyright (c) 2006-2011 by Public Library of Science
 *     http://plos.org
 *     http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ambraproject.rhino.model;

import java.util.Date;

/**
 * Base class for all persistent entities.  This holds the id, created and modified date, and manages updated those
 * timestamps.
 *
 * @author Alex Kudlick 11/9/11
 */
public abstract class AmbraEntity {
  private Long ID;

  private Date created;

  private Date lastModified;

  public AmbraEntity() {
    this.created = new Date();
    this.lastModified = new Date();
  }

  public Long getID() {
    return ID;
  }

  public void setID(Long ID) {
    this.ID = ID;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  public Date getLastModified() {
    return lastModified;
  }

  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() +
        "{" +
        "id=" + ID +
        ", created=" + created +
        ", lastModified=" + lastModified +
        '}';
  }
}
