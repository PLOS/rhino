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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
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

  @JoinColumn(name = "ingestionId", nullable = false)
  @ManyToOne
  private ArticleIngestion ingestion;

  @Column
  private String doi;

  @Column(name = "articleItemType")
  private String itemType;

  @Cascade(CascadeType.SAVE_UPDATE)
  @OneToMany(targetEntity = ArticleFile.class, mappedBy = "item")
  private Collection<ArticleFile> files;

  @Generated(value = GenerationTime.INSERT)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(name = "created", insertable = false, updatable = false, columnDefinition = "timestamp default current_timestamp")
  private Date created;


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
  public ImmutableSet<String> getFileTypes() {
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
