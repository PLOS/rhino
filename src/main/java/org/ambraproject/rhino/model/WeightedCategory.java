package org.ambraproject.rhino.model;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "articleCategoryJoinTable")
public class WeightedCategory {

  @EmbeddedId
  private WeightedCategoryId weightedCategoryId;
  @Column(name = "weight")
  private int weight;

  public WeightedCategory(WeightedCategoryId weightedCategoryId, int weight) {
    this.weightedCategoryId = weightedCategoryId;
    this.weight = weight;
  }

  public int getCategoryId() {
    return weightedCategoryId.getCategoryId();
  }

  public int getArticleId() {
    return weightedCategoryId.getArticleId();
  }

  public void setCategoryId(int categoryId) {
    weightedCategoryId.setCategoryId(categoryId);
  }

  public void setArticleId(int articleId) {
    weightedCategoryId.setArticleId(articleId);
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

    WeightedCategory that = (WeightedCategory) o;

    return weightedCategoryId != null ? weightedCategoryId.equals(that.weightedCategoryId) : that.weightedCategoryId == null;

  }

  @Override
  public int hashCode() {
    return weightedCategoryId != null ? weightedCategoryId.hashCode() : 0;
  }
}
