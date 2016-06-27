package org.ambraproject.rhino.model;

import org.plos.crepo.model.RepoVersion;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Date;

@Entity
@Table(name = "articleFile")
public class ArticleFile implements Timestamped {

  @Id
  @GeneratedValue
  @Column
  private long fileId;

  @JoinColumn(name = "ingestionId")
  @ManyToOne
  private ArticleIngestion ingestion;

  @JoinColumn(name = "itemId")
  @ManyToOne
  private ArticleItem item;

  @Column
  private String fileType;
  @Column
  private String crepoKey;
  @Column
  private String crepoUuid;

  @Column
  private Date created;


  public long getFileId() {
    return fileId;
  }

  public void setFileId(long fileId) {
    this.fileId = fileId;
  }

  public ArticleIngestion getIngestion() {
    return ingestion;
  }

  public void setIngestion(ArticleIngestion ingestion) {
    this.ingestion = ingestion;
  }

  public ArticleItem getItem() {
    return item;
  }

  public void setItem(ArticleItem item) {
    this.item = item;
  }

  public String getFileType() {
    return fileType;
  }

  public void setFileType(String fileType) {
    this.fileType = fileType;
  }

  public String getCrepoKey() {
    return crepoKey;
  }

  public void setCrepoKey(String crepoKey) {
    this.crepoKey = crepoKey;
  }

  public String getCrepoUuid() {
    return crepoUuid;
  }

  public void setCrepoUuid(String crepoUuid) {
    this.crepoUuid = crepoUuid;
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


  public void setCrepoVersion(RepoVersion crepoVersion) {
    this.crepoVersion = crepoVersion;
  }

  private transient RepoVersion crepoVersion;

  @Transient
  public RepoVersion getCrepoVersion() {
    return (crepoVersion != null) ? crepoVersion :
        (crepoVersion = RepoVersion.create(crepoKey, crepoUuid));
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleFile that = (ArticleFile) o;

    if (ingestion != null ? !ingestion.equals(that.ingestion) : that.ingestion != null) return false;
    if (crepoKey != null ? !crepoKey.equals(that.crepoKey) : that.crepoKey != null) return false;
    return crepoUuid != null ? crepoUuid.equals(that.crepoUuid) : that.crepoUuid == null;
  }

  @Override
  public int hashCode() {
    int result = ingestion != null ? ingestion.hashCode() : 0;
    result = 31 * result + (crepoKey != null ? crepoKey.hashCode() : 0);
    result = 31 * result + (crepoUuid != null ? crepoUuid.hashCode() : 0);
    return result;
  }
}
