package org.ambraproject.rhino.model;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import java.sql.Date;

@Entity
@Table(name = "articleIngestion")
public class ArticleIngestion implements Timestamped {

  @Id
  @GeneratedValue
  @Column
  private long ingestionId;

  @JoinColumn(name = "articleId", nullable = false)
  @ManyToOne
  private Article article;

  @Column
  private String title;

  @Column
  private Date publicationDate;

  @Column
  private Date revisionDate;

  @Column
  private int ingestionNumber;

  @Column
  private String articleType;

  @JoinColumn(name = "journalId", nullable = false)
  @ManyToOne
  private Journal journal;

  @JoinColumn(name = "strikingImageItemId", nullable = true)
  @OneToOne
  private ArticleItem strikingImage;

  @Generated(value= GenerationTime.ALWAYS)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(insertable=false, updatable=false, columnDefinition="timestamp default current_timestamp")
  private java.util.Date lastModified;


  public long getVersionId() {
    return ingestionId;
  }

  public void setVersionId(long ingestionId) {
    this.ingestionId = ingestionId;
  }

  public Article getArticle() {
    return article;
  }

  public void setArticle(Article article) {
    this.article = article;
  }

  public int getIngestionNumber() {
    return ingestionNumber;
  }

  public void setIngestionNumber(int ingestionNumber) {
    this.ingestionNumber = ingestionNumber;
  }

  public Journal getJournal() {
    return journal;
  }

  public void setJournal(Journal journal) {
    this.journal = journal;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Date getPublicationDate() {
    return publicationDate;
  }

  public void setPublicationDate(Date publicationDate) {
    this.publicationDate = publicationDate;
  }

  public Date getRevisionDate() {
    return revisionDate;
  }

  public void setRevisionDate(Date revisionDate) {
    this.revisionDate = revisionDate;
  }

  public String getArticleType() {
    return articleType;
  }

  public void setArticleType(String articleType) {
    this.articleType = articleType;
  }

  public ArticleItem getStrikingImage() {
    return strikingImage;
  }

  public void setStrikingImage(ArticleItem strikingImage) {
    this.strikingImage = strikingImage;
  }

  @Override
  public java.util.Date getLastModified() {
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
        '}';
  }
}
