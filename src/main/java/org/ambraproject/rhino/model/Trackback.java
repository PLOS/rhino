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

/**
 * @author Alex Kudlick 3/7/12
 */
public class Trackback extends Linkback {

  private String blogName;
  private String excerpt;

  public Trackback() {
    super();
  }

  public Trackback(Long articleID, String url) {
    super();
    setArticleID(articleID);
    setUrl(url);
  }

  public String getBlogName() {
    return blogName;
  }

  public void setBlogName(String blogName) {
    this.blogName = blogName;
  }

  public String getExcerpt() {
    return excerpt;
  }

  public void setExcerpt(String excerpt) {
    this.excerpt = excerpt;
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) return false;

    Trackback trackback = (Trackback) o;
    if (blogName != null ? !blogName.equals(trackback.blogName) : trackback.blogName != null) return false;
    if (excerpt != null ? !excerpt.equals(trackback.excerpt) : trackback.excerpt != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (blogName != null ? blogName.hashCode() : 0);
    result = 31 * result + (excerpt != null ? excerpt.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Trackback{" +
        "articleID=" + getArticleID() +
        ", url='" + getUrl() + '\'' +
        ", title='" + getTitle() + '\'' +
        ", blogName='" + blogName + '\'' +
        ", excerpt='" + excerpt + '\'' +
        '}';
  }
}
