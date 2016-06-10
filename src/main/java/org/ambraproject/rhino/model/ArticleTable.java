package org.ambraproject.rhino.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "article")
public class ArticleTable {

  @Id
  @GeneratedValue
  @Column(name = "articleId")
  private int articleId;

  @Column(name = "doi")
  private String doi;

}
