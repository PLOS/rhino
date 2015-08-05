package org.ambraproject.rhino.model;

import org.ambraproject.models.AmbraEntity;
import org.ambraproject.models.Article;
import org.ambraproject.models.Journal;

import java.util.List;

public class ArticleLink extends AmbraEntity {

  private String linkType;
  private String target;
  private String title;
  private Journal journal;
  private List<Article> articles;

  public String getLinkType() {
    return linkType;
  }

  public void setLinkType(String linkType) {
    this.linkType = linkType;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Journal getJournal() {
    return journal;
  }

  public void setJournal(Journal journal) {
    this.journal = journal;
  }

  public List<Article> getArticles() {
    return articles;
  }

  public void setArticles(List<Article> articles) {
    this.articles = articles;
  }

}
