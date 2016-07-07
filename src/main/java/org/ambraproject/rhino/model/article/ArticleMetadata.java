package org.ambraproject.rhino.model.article;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public class ArticleMetadata {

  private String doi;

  //simple properties
  private String title;
  private String eIssn;
  private String archiveName;
  private String description;
  private String rights;
  private String language;
  private String format;
  private String pages;
  private String eLocationId;
  private String strkImgURI;

  private LocalDate publicationDate;

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
  private List<AssetMetadata> assets;
  private List<Citation> citedArticles;
  private List<RelatedArticleLink> relatedArticles;
  private List<NlmPerson> authors;
  private List<NlmPerson> editors;

  public ArticleMetadata() {
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

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public String getPages() {
    return pages;
  }

  public void setPages(String pages) {
    this.pages = pages;
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

  public LocalDate getPublicationDate() {
    return publicationDate;
  }

  public void setPublicationDate(LocalDate publicationDate) {
    this.publicationDate = publicationDate;
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

  public String getJournal() {
    return journal;
  }

  public void setJournal(String journal) {
    this.journal = journal;
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

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public List<String> getCollaborativeAuthors() {
    return collaborativeAuthors;
  }

  public void setCollaborativeAuthors(List<String> collaborativeAuthors) {
    this.collaborativeAuthors = collaborativeAuthors;
  }

  public Set<String> getTypes() {
    return types;
  }

  public void setTypes(Set<String> types) {
    this.types = types;
  }

  public List<AssetMetadata> getAssets() {
    return assets;
  }

  public void setAssets(List<AssetMetadata> assets) {
    this.assets = assets;
  }

  public List<Citation> getCitedArticles() {
    return citedArticles;
  }

  public void setCitedArticles(List<Citation> citedArticles) {
    this.citedArticles = citedArticles;
  }

  public List<RelatedArticleLink> getRelatedArticles() {
    return relatedArticles;
  }

  public void setRelatedArticles(List<RelatedArticleLink> relatedArticles) {
    this.relatedArticles = relatedArticles;
  }

  public List<NlmPerson> getAuthors() {
    return authors;
  }

  public void setAuthors(List<NlmPerson> authors) {
    this.authors = authors;
  }

  public List<NlmPerson> getEditors() {
    return editors;
  }

  public void setEditors(List<NlmPerson> editors) {
    this.editors = editors;
  }
}
