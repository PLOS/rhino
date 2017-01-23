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

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "articleCategoryAssignmentFlag")
public class ArticleCategoryAssignmentFlag implements Serializable {

  @Id
  @GeneratedValue
  @Column
  private Long flagId;

  /*
   * The 'category' and 'article' fields together are a logical pointer to an ArticleCategoryAssignment object. The
   * database schema has a foreign key constraint that requires the pair of them to point to an existing
   * ArticleCategoryAssignment row, even though it is possible in Hibernate to point to a category that the article
   * isn't in.
   *
   * It would be better to refactor this class to remove the 'category' and 'article' fields and instead associate with
   * an ArticleCategoryAssignment object. This would probably use the @Embedded annotation and may necessitate a
   * compound ID class with Category and Article fields.
   */
  @JoinColumn(name = "categoryId", nullable = false)
  @ManyToOne
  private Category category;
  @JoinColumn(name = "articleId", nullable = false)
  @ManyToOne
  private Article article;

  @Column(nullable = true)
  private Long userProfileId;

  @Generated(value = GenerationTime.INSERT)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(name = "created", insertable = false, updatable = false, columnDefinition = "timestamp default current_timestamp")
  private Date created;


  public Long getFlagId() {
    return flagId;
  }

  public void setFlagId(Long flagId) {
    this.flagId = flagId;
  }

  public Category getCategory() {
    return category;
  }

  public void setCategory(Category category) {
    this.category = category;
  }

  public Article getArticle() {
    return article;
  }

  public void setArticle(Article article) {
    this.article = article;
  }

  public Long getUserProfileId() {
    return userProfileId;
  }

  public void setUserProfileId(Long userProfileId) {
    this.userProfileId = userProfileId;
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

    ArticleCategoryAssignmentFlag that = (ArticleCategoryAssignmentFlag) o;

    if (category != null ? !category.equals(that.category) : that.category != null) return false;
    if (article != null ? !article.equals(that.article) : that.article != null) return false;
    return userProfileId != null ? userProfileId.equals(that.userProfileId) : that.userProfileId == null;

  }

  @Override
  public int hashCode() {
    int result = category != null ? category.hashCode() : 0;
    result = 31 * result + (article != null ? article.hashCode() : 0);
    result = 31 * result + (userProfileId != null ? userProfileId.hashCode() : 0);
    return result;
  }
}
