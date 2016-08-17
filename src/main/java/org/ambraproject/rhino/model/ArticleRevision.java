package org.ambraproject.rhino.model;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import java.util.Date;

@Entity
@Table(name = "articleRevision")
public class ArticleRevision implements Timestamped {

  @Id
  @GeneratedValue
  @Column
  private long revisionId;

  @JoinColumn(name = "ingestionId", nullable = false)
  @OneToOne
  private ArticleIngestion ingestion;

  @Column
  private int revisionNumber;

  @Generated(value= GenerationTime.INSERT)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(insertable=false, updatable=false, columnDefinition="timestamp default current_timestamp")
  private Date created;

  public long getRevisionId() {
    return revisionId;
  }

  public void setRevisionId(long revisionId) {
    this.revisionId = revisionId;
  }

  public ArticleIngestion getIngestion() {
    return ingestion;
  }

  public void setIngestion(ArticleIngestion ingestion) {
    this.ingestion = ingestion;
  }

  public int getRevisionNumber() {
    return revisionNumber;
  }

  public void setRevisionNumber(int revisionNumber) {
    this.revisionNumber = revisionNumber;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  @Transient
  @Override
  public Date getLastModified() {
    return getCreated();
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleRevision that = (ArticleRevision) o;

    if (revisionId != that.revisionId) return false;
    if (revisionNumber != that.revisionNumber) return false;
    return ingestion != null ? ingestion.equals(that.ingestion) : that.ingestion == null;

  }

  @Override
  public int hashCode() {
    int result = (int) (revisionId ^ (revisionId >>> 32));
    result = 31 * result + (ingestion != null ? ingestion.hashCode() : 0);
    result = 31 * result + revisionNumber;
    return result;
  }

  @Override
  public String toString() {
    return "ArticleRevision{" +
        "revisionId=" + revisionId +
        ", ingestion=" + ingestion +
        ", revisionNumber=" + revisionNumber +
        '}';
  }
}
