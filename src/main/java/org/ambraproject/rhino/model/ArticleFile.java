/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.model;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import java.util.Date;

@Entity
@Table(name = "articleFile")
public class ArticleFile implements Timestamped {

  @Id
  @GeneratedValue
  @Column
  private long fileId;

  @JoinColumn(name = "ingestionId", nullable = false)
  @ManyToOne
  private ArticleIngestion ingestion;

  @JoinColumn(name = "itemId", nullable = true) // null if (and only if) this is an ancillary file
  @ManyToOne
  private ArticleItem item;

  @Column
  private String fileType;

  @Column
  private String bucketName;

  @Column
  private String crepoKey;

  @Column
  private String crepoUuid;

  @Column
  private long fileSize;

  @Column
  private String ingestedFileName;

  @Generated(value = GenerationTime.INSERT)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(name = "created", insertable = false, updatable = false, columnDefinition = "timestamp default current_timestamp")
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

  public String getBucketName() {
    return bucketName;
  }

  public void setBucketName(String bucketName) {
    this.bucketName = bucketName;
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

  public long getFileSize() {
    return fileSize;
  }

  public void setFileSize(long fileSize) {
    this.fileSize = fileSize;
  }

  public String getIngestedFileName() {
    return ingestedFileName;
  }

  public void setIngestedFileName(String ingestedFileName) {
    this.ingestedFileName = ingestedFileName;
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

  @Override
  public String toString() {
    return "ArticleFile{" +
        "bucketName='" + bucketName + '\'' +
        ", crepoKey='" + crepoKey + '\'' +
        ", crepoUuid='" + crepoUuid + '\'' +
        '}';
  }
}
