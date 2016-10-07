/*
 * Copyright (c) 2007-2014 by Public Library of Science
 *
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import java.util.Objects;

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

  public ArticleRelationship() {
    super();
  }

  public ArticleRelationship(Article sourceArticle, Article targetArticle, String type) {
    this();
    this.sourceArticle = Objects.requireNonNull(sourceArticle);
    this.targetArticle = Objects.requireNonNull(targetArticle);
    this.type = Objects.requireNonNull(type);
  }

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


}
