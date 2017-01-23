/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.model;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;
import javax.persistence.Temporal;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "articleList")
public class ArticleList implements Timestamped {

  @Id
  @GeneratedValue
  @Column
  private long articleListId;

  @Column
  private String listType;

  @Column
  private String listKey;

  @Column
  private String displayName;

  @Generated(value= GenerationTime.INSERT)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(insertable=false, updatable=false, columnDefinition="timestamp default current_timestamp")
  private Date created;

  @Generated(value= GenerationTime.ALWAYS)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(insertable=false, updatable=false, columnDefinition="timestamp default current_timestamp")
  private Date lastModified;

  @Cascade(CascadeType.SAVE_UPDATE)
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "articleListJoinTable",
      joinColumns = @JoinColumn(name = "articleListId", nullable = false),
      inverseJoinColumns = @JoinColumn(name = "articleId", nullable = false)
  )
  @OrderColumn(name="sortOrder", nullable=false)
  private List<Article> articles;

  public ArticleList() {
    super();
  }

  public ArticleList(String listKey) {
    super();
    this.listKey = listKey;
  }

  public long getArticleListId() {
    return articleListId;
  }

  public void setArticleListId(long articleListId) {
    this.articleListId = articleListId;
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
  public Date getLastModified() {
    return lastModified;
  }

  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleList articleList = (ArticleList) o;

    if (articleListId != articleList.articleListId) return false;
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
    int result = (int) (articleListId ^ (articleListId >>> 32));
    result = 31 * result + (listType != null ? listType.hashCode() : 0);
    result = 31 * result + (listKey != null ? listKey.hashCode() : 0);
    result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
    result = 31 * result + (articles != null ? articles.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ArticleList{" +
        "id='" + articleListId + '\'' +
        ", listType='" + listType + '\'' +
        ", listKey='" + listKey + '\'' +
        '}';
  }
}
