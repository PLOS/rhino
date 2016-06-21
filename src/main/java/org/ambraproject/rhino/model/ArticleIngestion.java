package org.ambraproject.rhino.model;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Set;

@Entity
@Table(name = "articleVersion")
public class ArticleIngestion extends AmbraEntity {

  @Id
  @GeneratedValue
  @Column
  private long versionId;

  @JoinColumn(name = "articleId")
  @ManyToOne
  private ArticleTable article;

  @Column
  private int revisionNumber;

  @Column
  private int publicationState;

  @Cascade(CascadeType.SAVE_UPDATE)
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "articleJournalJoinTable",
      joinColumns = @JoinColumn(name = "versionId"),
      inverseJoinColumns = @JoinColumn(name = "journalId")
  )
  private Set<Journal> journals;

  public long getVersionId() {
    return versionId;
  }

  public void setVersionId(long versionId) {
    this.versionId = versionId;
  }

  public ArticleTable getArticle() {
    return article;
  }

  public void setArticle(ArticleTable article) {
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

  public Set<Journal> getJournals() {
    return journals;
  }

  public void setJournals(Set<Journal> journals) {
    this.journals = journals;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleIngestion that = (ArticleIngestion) o;

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
