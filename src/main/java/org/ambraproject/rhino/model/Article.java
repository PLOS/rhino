/*
 * Copyright (c) 2007-2014 by Public Library of Science
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
package org.ambraproject.rhino.model;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Model class containing all the information for an article
 *
 * @author Alex Kudlick 11/8/11
 */
public class Article extends AmbraEntity {
  /**
   * Article state of "Active"
   */
  public static final int STATE_ACTIVE = 0;
  /**
   * Article state of "Unpublished"
   */
  public static final int STATE_UNPUBLISHED = 1;
  /**
   * Article state of "Disabled"
   */
  public static final int STATE_DISABLED = 2;
  /**
   * Active article states
   */
  public static final int[] ACTIVE_STATES = {STATE_ACTIVE};

  private String doi;

  //simple properties
  private String title;
  private String eIssn;
  private int state;
  private String archiveName;
  private String description;
  private String rights;
  private String language;
  private String format;
  private String pages;
  private String eLocationId;
  private String strkImgURI;

  private Date date;

  private String volume;
  private String issue;
  private String journal;

  private String publisherLocation;
  private String publisherName;
  private String url;

  //simple collections
  private List<String> collaborativeAuthors;
  private Set<String> types;

  //collections of persistent objects
  private Map<Category, Integer> categories;
  private List<ArticleAsset> assets;
  private List<CitedArticle> citedArticles;
  private List<ArticleRelationship> relatedArticles;
  private List<ArticleAuthor> authors;
  private List<ArticleEditor> editors;
  private Set<Journal> journals;


  /**
   * The class constructor.  It calls the super class.
   *
   * @return An instance of Article
   */
  public Article() {
    super();
  }

  public Set<Journal> getJournals() {
    return journals;
  }

  public void setJournals(Set<Journal> journals) {
    this.journals = journals;
  }

  public void addJournal(Journal j) {
    journals.add(j);
  }

  public void removeJournal(Journal j) {
    journals.remove(j);
  }

  public Article(String doi) {
    super();
    this.doi = doi;
  }

  public String getDoi() {
    return doi;
  }

  public void setDoi(String doi) {
    this.doi = doi;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String geteIssn() {
    return eIssn;
  }

  public void seteIssn(String eIssn) {
    this.eIssn = eIssn;
  }

  public String geteLocationId() {
    return eLocationId;
  }

  public void seteLocationId(String eLocationId) {
    this.eLocationId = eLocationId;
  }

  public String getStrkImgURI() {
    return strkImgURI;
  }

  public void setStrkImgURI(String strkImgURI) {
    this.strkImgURI = strkImgURI;
  }

  public int getState() {
    return state;
  }

  public void setState(int state) {
    this.state = state;
  }

  public String getArchiveName() {
    return archiveName;
  }

  public void setArchiveName(String archiveName) {
    this.archiveName = archiveName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getRights() {
    return rights;
  }

  public void setRights(String rights) {
    this.rights = rights;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public String getPages() {
    return pages;
  }

  public void setPages(String pages) {
    this.pages = pages;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getVolume() {
    return volume;
  }

  public void setVolume(String volume) {
    this.volume = volume;
  }

  public String getIssue() {
    return issue;
  }

  public void setIssue(String issue) {
    this.issue = issue;
  }

  public String getPublisherLocation() {
    return publisherLocation;
  }

  public void setPublisherLocation(String publisherLocation) {
    this.publisherLocation = publisherLocation;
  }

  public String getPublisherName() {
    return publisherName;
  }

  public void setPublisherName(String publisherName) {
    this.publisherName = publisherName;
  }

  public String getJournal() {
    return journal;
  }

  public void setJournal(String journal) {
    this.journal = journal;
  }

  public List<String> getCollaborativeAuthors() {
    return collaborativeAuthors;
  }

  public void setCollaborativeAuthors(List<String> collaborativeAuthors) {
    this.collaborativeAuthors = collaborativeAuthors;
  }

  public Map<Category, Integer> getCategories() {
    return categories;
  }

  public void setCategories(Map<Category, Integer> categories) {
    this.categories = categories;
  }

  public List<ArticleAsset> getAssets() {
    return assets;
  }

  public void setAssets(List<ArticleAsset> assets) {
    this.assets = assets;
  }

  public List<CitedArticle> getCitedArticles() {
    return citedArticles;
  }

  public void setCitedArticles(List<CitedArticle> citedArticles) {
    this.citedArticles = citedArticles;
  }

  public List<ArticleAuthor> getAuthors() {
    return authors;
  }

  public void setAuthors(List<ArticleAuthor> authors) {
    this.authors = authors;
  }

  public List<ArticleEditor> getEditors() {
    return editors;
  }

  public void setEditors(List<ArticleEditor> editors) {
    this.editors = editors;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public List<ArticleRelationship> getRelatedArticles() {
    return relatedArticles;
  }

  public void setRelatedArticles(List<ArticleRelationship> relatedArticles) {
    this.relatedArticles = relatedArticles;
  }

  public Set<String> getTypes() {
    return types;
  }

  public void setTypes(Set<String> types) {
    this.types = types;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Article)) return false;


    Article article = (Article) o;

    if (getID() != null ? !getID().equals(article.getID()) : article.getID() != null) return false;
    if (doi != null ? !doi.equals(article.doi) : article.doi != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = getID() != null ? getID().hashCode() : 0;
    result = 31 * result + (doi != null ? doi.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Article{" +
        "doi='" + doi + '\'' +
        '}';
  }
}
