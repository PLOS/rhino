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
import javax.persistence.Table;
import javax.persistence.Temporal;
import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "article")
public class ArticleTable implements Timestamped { //todo: rename to "Article" once the old Article class is removed

  @Id
  @GeneratedValue
  @Column
  private Long articleId;

  @Column
  private String doi;

  @Generated(value = GenerationTime.INSERT)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(name = "created", insertable = false, updatable = false, columnDefinition = "timestamp default current_timestamp")
  private Date publicationDate;

  @Cascade(CascadeType.SAVE_UPDATE)
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "articleCategoryJoinTable",
      joinColumns = @JoinColumn(name = "categoryId"),
      inverseJoinColumns = @JoinColumn(name = "articleId")
  )
  private Set<Category> categories;

  public Long getArticleId() {
    return articleId;
  }

  public void setArticleId(Long articleId) {
    this.articleId = articleId;
  }

  public String getDoi() {
    return doi;
  }

  public void setDoi(String doi) {
    this.doi = doi;
  }

  public Date getPublicationDate() {
    return publicationDate;
  }

  public void setPublicationDate(Date publicationDate) {
    this.publicationDate = publicationDate;
  }

  public Set<Category> getCategories() {
    return categories;
  }

  public void setCategories(Set<Category> categories) {
    this.categories = categories;
  }

  @Override
  public Date getLastModified() {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleTable that = (ArticleTable) o;

    if (articleId != that.articleId) return false;
    return doi.equals(that.doi);

  }

  @Override
  public int hashCode() {
    int result = Long.hashCode(articleId);
    result = 31 * result + doi.hashCode();
    return result;
  }
}
