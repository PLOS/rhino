/*
 * Copyright (c) 2006-2014 by Public Library of Science
 *
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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
package org.ambraproject.rhino.view.article;

import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.models.ArticleAuthor;
import org.ambraproject.models.ArticleEditor;
import org.ambraproject.models.ArticleRelationship;
import org.ambraproject.models.Category;
import org.ambraproject.models.CitedArticle;
import org.ambraproject.models.Journal;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * This class mocks the response of article categories.  As the response
 * Does not directly match the model; the categories in the response,
 * are not a map including category weight.
 */
public class ArticleTestView implements ArticleView {
  private final Long ID;
  private final String doi;

  //simple properties
  private final String title;
  private final String eIssn;
  private final int state;
  private final String archiveName;
  private final String description;
  private final String rights;
  private final String language;
  private final String format;
  private final String pages;
  private final String eLocationId;
  private final String strkImgURI;
  private final Date date;
  private final String volume;
  private final String issue;
  private final String journal;
  private final String publisherLocation;
  private final String publisherName;
  private final String url;
  private final List<String> collaborativeAuthors;
  private final Set<String> types;
  private final Set<Category> categories;
  private final List<ArticleAsset> assets;
  private final List<CitedArticle> citedArticles;
  private final List<ArticleRelationship> relatedArticles;
  private final List<ArticleAuthor> authors;
  private final List<ArticleEditor> editors;
  private final Set<Journal> journals;
  private final Date created;
  private final Date lastModified;

  public ArticleTestView(Article article) {
    this.ID = article.getID();
    this.doi = article.getDoi();
    this.title = article.getTitle();
    this.eIssn = article.geteIssn();
    this.state = article.getState();
    this.archiveName = article.getArchiveName();
    this.description = article.getDescription();
    this.rights = article.getRights();
    this.language = article.getLanguage();
    this.format = article.getFormat();
    this.pages = article.getPages();
    this.eLocationId = article.geteLocationId();
    this.strkImgURI = article.getStrkImgURI();
    this.date = article.getDate();
    this.volume = article.getVolume();
    this.issue = article.getIssue();
    this.journal = article.getJournal();
    this.publisherLocation = article.getPublisherLocation();
    this.publisherName = article.getPublisherName();
    this.url = article.getUrl();
    this.collaborativeAuthors = article.getCollaborativeAuthors();
    this.types = article.getTypes();
    this.categories = article.getCategories().keySet();
    this.assets = article.getAssets();
    this.citedArticles = article.getCitedArticles();
    this.relatedArticles = article.getRelatedArticles();
    this.authors = article.getAuthors();
    this.editors = article.getEditors();
    this.journals = article.getJournals();
    this.created = article.getCreated();
    this.lastModified = article.getLastModified();
  }

  public Long getID() {
    return ID;
  }

  public String getDoi() {
    return doi;
  }

  public String getTitle() {
    return title;
  }

  public String geteIssn() {
    return eIssn;
  }

  public int getState() {
    return state;
  }

  public String getArchiveName() {
    return archiveName;
  }

  public String getDescription() {
    return description;
  }

  public String getRights() {
    return rights;
  }

  public String getLanguage() {
    return language;
  }

  public String getFormat() {
    return format;
  }

  public String getPages() {
    return pages;
  }

  public String geteLocationId() {
    return eLocationId;
  }

  public String getStrkImgURI() {
    return strkImgURI;
  }

  public Date getDate() {
    return date;
  }

  public String getVolume() {
    return volume;
  }

  public String getIssue() {
    return issue;
  }

  public String getJournal() {
    return journal;
  }

  public String getPublisherLocation() {
    return publisherLocation;
  }

  public String getPublisherName() {
    return publisherName;
  }

  public String getUrl() {
    return url;
  }

  public List<String> getCollaborativeAuthors() {
    return collaborativeAuthors;
  }

  public Set<String> getTypes() {
    return types;
  }

  public Set<Category> getCategories() {
    return categories;
  }

  public List<ArticleAsset> getAssets() {
    return assets;
  }

  public List<CitedArticle> getCitedArticles() {
    return citedArticles;
  }

  public List<ArticleRelationship> getRelatedArticles() {
    return relatedArticles;
  }

  public List<ArticleAuthor> getAuthors() {
    return authors;
  }

  public List<ArticleEditor> getEditors() {
    return editors;
  }

  public Set<Journal> getJournals() {
    return journals;
  }

  public Date getCreated() {
    return created;
  }

  public Date getLastModified() {
    return lastModified;
  }
}
