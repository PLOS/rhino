package org.ambraproject.rhino.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "articleCategoryAssignment")
public class ArticleCategoryAssignment implements Serializable {

  @Id
  @JoinColumn(name = "categoryId", nullable = false)
  @ManyToOne
  private Category category;
  @Id
  @JoinColumn(name = "articleId", nullable = false)
  @ManyToOne
  private ArticleTable article;

  @Column
  private int weight;

  public ArticleCategoryAssignment() {
  }

  public ArticleCategoryAssignment(Category category, ArticleTable article, int weight) {
    this.category = category;
    this.article = article;
    this.weight = weight;
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

  public int getWeight() {
    return weight;
  }

  public void setWeight(int weight) {
    this.weight = weight;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleCategoryAssignment that = (ArticleCategoryAssignment) o;

    if (!category.equals(that.category)) return false;
    return article.equals(that.article);

  }

  @Override
  public int hashCode() {
    int result = category.hashCode();
    result = 31 * result + article.hashCode();
    return result;
  }
}
