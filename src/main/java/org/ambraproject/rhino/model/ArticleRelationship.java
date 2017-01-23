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
import java.util.Date;

/**
 * An outgoing relationship between two articles (e.g. for corrections, sourceArticle = correction article,
 * targetArticle = original article). The corresponding incoming relationships are not persisted in order to
 * avoid redundancy and potential data integrity headaches.
 *
 * @author John Fesenko 6/28/2016
 */
@Entity
@Table(name = "articleRelationship")
public class ArticleRelationship implements Timestamped {

  @Id
  @GeneratedValue
  @Column
  private int articleRelationshipId;

  @ManyToOne
  @JoinColumn(name = "sourceArticleId", nullable = false)
  private Article sourceArticle;

  @ManyToOne
  @JoinColumn(name = "targetArticleId", nullable = false)
  private Article targetArticle;

  @Column
  private String type;

  @Generated(value = GenerationTime.INSERT)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(name = "created", insertable = false, updatable = false, columnDefinition = "timestamp default current_timestamp")
  private Date created;

  @Generated(value= GenerationTime.ALWAYS)
  @Temporal(javax.persistence.TemporalType.TIMESTAMP)
  @Column(insertable=false, updatable=false, columnDefinition="timestamp default current_timestamp")
  private Date lastModified;

  public Article getSourceArticle() {
    return sourceArticle;
  }

  public void setSourceArticle(Article sourceArticle) {
    this.sourceArticle = sourceArticle;
  }

  public Article getTargetArticle() {
    return targetArticle;
  }

  public void setTargetArticle(Article targetArticle) {
    this.targetArticle = targetArticle;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
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

    ArticleRelationship that = (ArticleRelationship) o;

    if (sourceArticle != null ? !sourceArticle.equals(that.sourceArticle) : that.sourceArticle != null) return false;
    if (targetArticle != null ? !targetArticle.equals(that.targetArticle) : that.targetArticle != null) return false;
    return type != null ? type.equals(that.type) : that.type == null;

  }

  @Override
  public int hashCode() {
    int result = sourceArticle != null ? sourceArticle.hashCode() : 0;
    result = 31 * result + (targetArticle != null ? targetArticle.hashCode() : 0);
    result = 31 * result + (type != null ? type.hashCode() : 0);
    return result;
  }
}
