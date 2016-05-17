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

import org.ambraproject.rhino.config.PersistenceAdapter;

/**
 * Class representing an article view or download by a logged in user.
 *
 * @author Alex Kudlick 2/16/12
 */
public class ArticleView extends AmbraEntity {

  public static enum Type {
    ARTICLE_VIEW,
    PDF_DOWNLOAD,
    XML_DOWNLOAD
  }

  public static final PersistenceAdapter<Type, String> TYPE_ADAPTER = PersistenceAdapter.byEnumName(Type.class);

  public ArticleView() {
    super();
  }

  public ArticleView(Long userID, Long articleID, Type type) {
    this.articleID = articleID;
    this.userID = userID;
    this.type = type;
  }

  private Long articleID;
  private Long userID;
  private Type type;

  public Long getArticleID() {
    return articleID;
  }

  public void setArticleID(Long articleID) {
    this.articleID = articleID;
  }

  public Long getUserID() {
    return userID;
  }

  public void setUserID(Long userID) {
    this.userID = userID;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ArticleView)) return false;

    ArticleView that = (ArticleView) o;

    if (getID() != null ? !getID().equals(that.getID()) : that.getID() != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return getID() != null ? getID().hashCode() : 0;
  }

  @Override
  public String toString() {
    return "ArticleView{" +
        "articleID=" + articleID +
        ", userID=" + userID +
        ", type=" + type +
        ", date=" + getCreated() +
        '}';
  }
}
