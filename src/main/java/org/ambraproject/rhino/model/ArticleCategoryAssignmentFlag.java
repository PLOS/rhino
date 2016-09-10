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

  @JoinColumn(name = "categoryId", nullable = false)
  @ManyToOne
  private Category category;
  @JoinColumn(name = "articleId", nullable = false)
  @ManyToOne
  private ArticleTable article;

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

  public ArticleTable getArticle() {
    return article;
  }

  public void setArticle(ArticleTable article) {
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
