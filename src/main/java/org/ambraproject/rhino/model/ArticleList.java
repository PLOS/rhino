/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2013 by Public Library of Science http://plos.org http://ambraproject.org
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

public class ArticleList extends AmbraEntity {

  private String listType;
  private String listKey;
  private String displayName;

  private List<Article> articles;

  public ArticleList() {
    super();
  }

  public ArticleList(String listKey) {
    super();
    this.listKey = listKey;
  }

  public String getListType() {
    return listType;
  }

  public void setListType(String listType) {
    this.listType = listType;
  }

  public String getListKey() {
    return listKey;
  }

  public void setListKey(String listKey) {
    this.listKey = listKey;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public List<Article> getArticles() {
    return articles;
  }

  public void setArticles(List<Article> articles) {
    this.articles = articles;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleList articleList = (ArticleList) o;

    if (getID() != null ? !getID().equals(articleList.getID()) : articleList.getID() != null) return false;
    if (articles != null ? !articles.equals(articleList.articles) : articleList.articles != null) return false;
    if (displayName != null ? !displayName.equals(articleList.displayName) : articleList.displayName != null) {
      return false;
    }
    if (listKey != null ? !listKey.equals(articleList.listKey) : articleList.listKey != null) return false;
    if (listType != null ? !listType.equals(articleList.listType) : articleList.listType != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = getID() != null ? getID().hashCode() : 0;
    result = 31 * result + (listType != null ? listType.hashCode() : 0);
    result = 31 * result + (listKey != null ? listKey.hashCode() : 0);
    result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
    result = 31 * result + (articles != null ? articles.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ArticleList{" +
        "id='" + getID() + '\'' +
        ", listType='" + listType + '\'' +
        ", listKey='" + listKey + '\'' +
        '}';
  }
}
