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

//todo: figure this out if possible to get list of comments on article w/o a new table or column
//  @Cascade(CascadeType.SAVE_UPDATE)
//  @ManyToMany(fetch = FetchType.LAZY)
//  @JoinTable(
//      name = "comment",
//      joinColumns = @JoinColumn(name = "commentId"),
//      inverseJoinColumns = @JoinColumn(name = "articleId")
//  )
//  private Set<Comment> comments;

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

//  public Set<Comment> getComments() {
//    return comments;
//  }
//
//  public void setComments(Set<Comment> comments) {
//    this.comments = comments;
//  }

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
