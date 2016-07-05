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

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
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
public class VersionedArticleRelationship implements Timestamped {

  @ManyToOne
  @JoinColumn(name = "sourceArticleId")
  private ArticleTable sourceArticle;

  @ManyToOne
  @JoinColumn(name = "targetArticleId")
  private ArticleTable targetArticle;

  @Column
  @Type(type = "org.ambraproject.rhino.config.HibernateAdaptingType",
      parameters = {@Parameter(name = "class", value = "org.ambraproject.rhino.model.ArticleRelationshipType"),
          @Parameter(name = "adapter", value = "ADAPTER")})
  private ArticleRelationshipType type;

  @Column
  private Date created;

  @Column
  private Date lastModified;

  public ArticleTable getSourceArticle() {
    return sourceArticle;
  }

  public void setSourceArticle(ArticleTable sourceArticle) {
    this.sourceArticle = sourceArticle;
  }

  public ArticleTable getTargetArticle() {
    return targetArticle;
  }

  public void setTargetArticle(ArticleTable targetArticle) {
    this.targetArticle = targetArticle;
  }

  public ArticleRelationshipType getType() {
    return type;
  }

  public void setType(ArticleRelationshipType type) {
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
