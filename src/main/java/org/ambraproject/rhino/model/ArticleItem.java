package org.ambraproject.rhino.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;

@Entity
@Table(name = "articleItem")
public class ArticleItem implements Timestamped {

  @Id
  @GeneratedValue
  @Column
  private long itemId;

  @JoinColumn(name = "ingestionId")
  @ManyToOne
  private ArticleIngestion ingestion;

  @Column
  private String doi;

  @Column(name = "articleItemType")
  private String itemType;

  @Cascade(CascadeType.SAVE_UPDATE)
  @OneToMany(targetEntity = ArticleFile.class, mappedBy = "item")
  private Collection<ArticleFile> files;

  @Column
  private Date lastModified;


  public long getItemId() {
    return itemId;
  }

  public void setItemId(long itemId) {
    this.itemId = itemId;
  }

  public ArticleIngestion getIngestion() {
    return ingestion;
  }

  public void setIngestion(ArticleIngestion ingestion) {
    this.ingestion = ingestion;
  }

  public String getDoi() {
    return doi;
  }

  public void setDoi(String doi) {
    this.doi = doi;
  }

  public String getItemType() {
    return itemType;
  }

  public void setItemType(String itemType) {
    this.itemType = itemType;
  }

  public Collection<ArticleFile> getFiles() {
    return files;
  }

  public void setFiles(Collection<ArticleFile> files) {
    this.files = files;
  }

  @Override
  public Date getLastModified() {
    return lastModified;
  }

  public void setLastModified(Date lastModified) {
    this.lastModified = lastModified;
  }


  private transient ImmutableMap<String, ArticleFile> fileMap;

  @Transient
  private ImmutableMap<String, ArticleFile> getFileMap() {
    return (fileMap != null) ? fileMap :
        (fileMap = Maps.uniqueIndex(getFiles(), ArticleFile::getFileType));
  }

  @Transient
  public Optional<ArticleFile> getFile(String fileType) {
    return Optional.ofNullable(getFileMap().get(fileType));
  }

  @Transient
  public ImmutableSet<String> getFileTypes(String fileType) {
    return getFileMap().keySet();
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleItem that = (ArticleItem) o;

    if (ingestion != null ? !ingestion.equals(that.ingestion) : that.ingestion != null) return false;
    return doi != null ? doi.equals(that.doi) : that.doi == null;

  }

  @Override
  public int hashCode() {
    int result = ingestion != null ? ingestion.hashCode() : 0;
    result = 31 * result + (doi != null ? doi.hashCode() : 0);
    return result;
  }
}
