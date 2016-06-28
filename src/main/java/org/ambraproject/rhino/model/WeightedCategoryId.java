package org.ambraproject.rhino.model;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class WeightedCategoryId implements Serializable {

  @Column(name = "categoryId")
  private int categoryId;
  @Column(name = "articleId")
  private int articleId;

  public WeightedCategoryId(int categoryId, int articleId) {
    this.categoryId = categoryId;
    this.articleId = articleId;
  }

  public int getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(int categoryId) {
    this.categoryId = categoryId;
  }

  public int getArticleId() {
    return articleId;
  }

  public void setArticleId(int articleId) {
    this.articleId = articleId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    WeightedCategoryId id = (WeightedCategoryId) o;

    if (categoryId != id.categoryId) return false;
    return articleId == id.articleId;

  }

  @Override
  public int hashCode() {
    int result = categoryId;
    result = 31 * result + articleId;
    return result;
  }
}
