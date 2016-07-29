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
import java.util.Date;
import java.util.Set;

@Entity
@Table(name = "articleIngestion")
public class ArticleIngestion implements Timestamped {

  @Id
  @GeneratedValue
  @Column
  private long ingestionId;

  @JoinColumn(name = "articleId")
  @ManyToOne
  private ArticleTable article;

  @Column
  private String articleTitle;

  @Column
  private Date publicationDate;

  @Column
  private int ingestionNumber;

  @Cascade(CascadeType.SAVE_UPDATE)
  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "articleJournalJoinTable",
      joinColumns = @JoinColumn(name = "ingestionId"),
      inverseJoinColumns = @JoinColumn(name = "journalId")
  )
  private Set<Journal> journals;

  @Column
  private Date lastModified;


  public long getVersionId() {
    return ingestionId;
  }

  public void setVersionId(long ingestionId) {
    this.ingestionId = ingestionId;
  }

  public ArticleTable getArticle() {
    return article;
  }

  public void setArticle(ArticleTable article) {
    this.article = article;
  }

  public int getIngestionNumber() {
    return ingestionNumber;
  }

  public void setIngestionNumber(int ingestionNumber) {
    this.ingestionNumber = ingestionNumber;
  }

  public Set<Journal> getJournals() {
    return journals;
  }

  public void setJournals(Set<Journal> journals) {
    this.journals = journals;
  }

  public String getArticleTitle() {
    return articleTitle;
  }

  public void setArticleTitle(String articleTitle) {
    this.articleTitle = articleTitle;
  }

  public Date getPublicationDate() {
    return publicationDate;
  }

  public void setPublicationDate(Date publicationDate) {
    this.publicationDate = publicationDate;
  }

  @Override
  public Date getLastModified() {
    return lastModified;
  }

  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleIngestion that = (ArticleIngestion) o;

    if (ingestionNumber != that.ingestionNumber) return false;
    return article != null ? article.equals(that.article) : that.article == null;
  }

  @Override
  public int hashCode() {
    int result = article != null ? article.hashCode() : 0;
    result = 31 * result + ingestionNumber;
    return result;
  }


  @Override
  public String toString() {
    return "ArticleIngestion{" +
        "ingestionId=" + ingestionId +
        ", article=" + article +
        ", journals=" + journals +
        '}';
  }
}
