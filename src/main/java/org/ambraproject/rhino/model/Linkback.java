/*
 * $HeadURL$
 * $Id$
 *
 * Copyright (c) 2007-2012 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
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

/**
 * A notification from an outside website that a link to an article exists. There is one concrete subclass for each
 * supported protocol.
 */
public abstract class Linkback extends AmbraEntity {

  private Long articleID;
  private String url;
  private String title;

  public Long getArticleID() {
    return articleID;
  }

  public void setArticleID(Long articleID) {
    this.articleID = articleID;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Linkback linkback = (Linkback) o;

    if (articleID != null ? !articleID.equals(linkback.articleID) : linkback.articleID != null) return false;
    if (title != null ? !title.equals(linkback.title) : linkback.title != null) return false;
    if (url != null ? !url.equals(linkback.url) : linkback.url != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = articleID != null ? articleID.hashCode() : 0;
    result = 31 * result + (url != null ? url.hashCode() : 0);
    result = 31 * result + (title != null ? title.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append("{articleID=").append(articleID);
    sb.append(", url='").append(url).append('\'');
    sb.append(", title='").append(title).append('\'');
    sb.append('}');
    return sb.toString();
  }
}