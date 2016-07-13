package org.ambraproject.rhino.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "issueArticleList")
public class IssueArticle implements Serializable {

  @Id
  @JoinColumn(name = "issueId")
  @ManyToOne
  private Issue issue;
  @Id
  @JoinColumn(name = "articleId")
  @ManyToOne
  private ArticleTable article;

  @Column
  private int sortOrder;

  public IssueArticle() {
  }

  public IssueArticle(Issue issue, ArticleTable article, int sortOrder) {
    this.issue= issue;
    this.article = article;
    this.sortOrder = sortOrder;
  }

  public Issue getIssue() {
    return issue;
  }

  public void setIssue(Issue issue) {
    this.issue = issue;
  }

  public ArticleTable getArticle() {
    return article;
  }

  public void setArticle(ArticleTable article) {
    this.article = article;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    IssueArticle that = (IssueArticle) o;

    if (sortOrder != that.sortOrder) return false;
    if (issue != null ? !issue.equals(that.issue) : that.issue != null) return false;
    return article != null ? article.equals(that.article) : that.article == null;

  }

  @Override
  public int hashCode() {
    int result = issue != null ? issue.hashCode() : 0;
    result = 31 * result + (article != null ? article.hashCode() : 0);
    result = 31 * result + sortOrder;
    return result;
  }
}
