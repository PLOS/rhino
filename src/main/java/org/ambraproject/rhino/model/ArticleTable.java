package org.ambraproject.rhino.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "article")
public class ArticleTable { //todo: rename to "Article" once the old Article class is removed

  @Id
  @GeneratedValue
  @Column(name = "articleId")
  private Long articleId;

  @Column(name = "doi")
  private String doi;

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
    Long result = articleId;
    result = 31 * result + doi.hashCode();
    return result.intValue();
  }
}
