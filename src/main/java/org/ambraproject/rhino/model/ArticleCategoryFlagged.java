/*
 * Copyright (c) 2006-2013 by Public Library of Science
 *
 *   http://plos.org
 *   http://ambraproject.org
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

import java.io.Serializable;
import java.util.Date;

/**
 * Never really populated at runtime, but this is here for the unit test framework to be aware of the table
 */
public class ArticleCategoryFlagged implements Serializable {
  private Long articleID;
  private Long userProfileID;
  private Long categoryID;
  private Date created;
  private Date lastModified;

  public Long getArticleID() {
    return articleID;
  }

  public void setArticleID(Long articleID) {
    this.articleID = articleID;
  }

  public Long getUserProfileID() {
    return userProfileID;
  }

  public void setUserProfileID(Long userProfileID) {
    this.userProfileID = userProfileID;
  }

  public Long getCategoryID() {
    return categoryID;
  }

  public void setCategoryID(Long categoryID) {
    this.categoryID = categoryID;
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
}
