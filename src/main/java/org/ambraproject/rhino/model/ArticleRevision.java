package org.ambraproject.rhino.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "articleIngestion")
public class ArticleRevision {

  @Id
  @GeneratedValue
  @Column
  private long versionId;

  @JoinColumn(name = "ingestionId")
  @OneToOne
  private ArticleIngestion ingestion;

  private int revisionNumber;

  public long getVersionId() {
    return versionId;
  }

  public void setVersionId(long versionId) {
    this.versionId = versionId;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleRevision that = (ArticleRevision) o;

    if (versionId != that.versionId) return false;
    if (revisionNumber != that.revisionNumber) return false;
    return ingestion != null ? ingestion.equals(that.ingestion) : that.ingestion == null;

  }

  @Override
  public int hashCode() {
    int result = (int) (versionId ^ (versionId >>> 32));
    result = 31 * result + (ingestion != null ? ingestion.hashCode() : 0);
    result = 31 * result + revisionNumber;
    return result;
  }

  @Override
  public String toString() {
    return "ArticleRevision{" +
        "versionId=" + versionId +
        ", ingestion=" + ingestion +
        ", revisionNumber=" + revisionNumber +
        '}';
  }
}
