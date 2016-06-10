package org.ambraproject.rhino.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "articleVersion")
public class ArticleVersion extends AmbraEntity {

  @Id
  @GeneratedValue
  @Column(name = "versionId")
  private long versionId;

  @JoinColumn(name = "articleId")
  @ManyToOne
  private Article article;

  @Column(name = "revisionNumber")
  private int revisionNumber;

  @Column(name = "publicationState")
  private int publicationState;

  public long getVersionId() {
    return versionId;
  }

  public void setVersionId(long versionId) {
    this.versionId = versionId;
  }

  public Article getArticle() {
    return article;
  }

  public void setArticle(Article article) {
    this.article = article;
  }

  public int getRevisionNumber() {
    return revisionNumber;
  }

  public void setRevisionNumber(int revisionNumber) {
    this.revisionNumber = revisionNumber;
  }

  public int getPublicationState() {
    return publicationState;
  }

  public void setPublicationState(int publicationState) {
    this.publicationState = publicationState;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleVersion that = (ArticleVersion) o;

    if (versionId != that.versionId) return false;
    if (revisionNumber != that.revisionNumber) return false;
    if (publicationState != that.publicationState) return false;
    return article.equals(that.article);

  }

  @Override
  public int hashCode() {
    int result = (int) (versionId ^ (versionId >>> 32));
    result = 31 * result + article.hashCode();
    result = 31 * result + revisionNumber;
    result = 31 * result + publicationState;
    return result;
  }

  @Override
  public String toString() {
    return "ArticleVersion{" +
        "versionId=" + versionId +
        ", article=" + article +
        ", revisionNumber=" + revisionNumber +
        ", publicationState=" + publicationState +
        '}';
  }
}
