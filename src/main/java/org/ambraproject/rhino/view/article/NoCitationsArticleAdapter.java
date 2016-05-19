/*
 * Copyright (c) 2006-2014 by Public Library of Science
 *
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ambraproject.rhino.view.article;

import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleAsset;
import org.ambraproject.rhino.model.ArticleAuthor;
import org.ambraproject.rhino.model.ArticleEditor;
import org.ambraproject.rhino.model.ArticleRelationship;
import org.ambraproject.rhino.model.Journal;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Simple adapter for {@link Article} that hides the citedArticles field.  Needed since the gson library will serialize
 * everything, and sometimes we don't want it to trigger hibernate lazy loading on this field for performance reasons.
 */
public final class NoCitationsArticleAdapter {

  private String doi;
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
  private List<String> collaborativeAuthors;
  private Set<String> types;
  private List<ArticleAsset> assets;
  private List<ArticleRelationship> relatedArticles;
  private List<ArticleAuthor> authors;
  private List<ArticleEditor> editors;
  // Omit Set<Journal> journals: it's handled in ArticleOutputView.serialize() via JournalNonAssocView
  // Omit Map<Category, Integer> categories: it's handled in ArticleOutputView.serialize() via CategoryView

  public NoCitationsArticleAdapter(Article article) {
    doi = article.getDoi();
    title = article.getTitle();
    eIssn = article.geteIssn();
    state = article.getState();
    archiveName = article.getArchiveName();
    description = article.getDescription();
    rights = article.getRights();
    language = article.getLanguage();
    format = article.getFormat();
    pages = article.getPages();
    eLocationId = article.geteLocationId();
    strkImgURI = article.getStrkImgURI();
    date = article.getDate();
    volume = article.getVolume();
    issue = article.getIssue();
    journal = article.getJournal();
    publisherLocation = article.getPublisherLocation();
    publisherName = article.getPublisherName();
    url = article.getUrl();
    collaborativeAuthors = article.getCollaborativeAuthors();
    types = article.getTypes();
    assets = article.getAssets();
    relatedArticles = article.getRelatedArticles();
    authors = article.getAuthors();
    editors = article.getEditors();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NoCitationsArticleAdapter)) {
      return false;
    }
    NoCitationsArticleAdapter article = (NoCitationsArticleAdapter) o;
    return doi.equals(article.doi);
  }

  @Override
  public int hashCode() {
    return doi.hashCode();
  }
}
