package org.ambraproject.rhino.model.article;

import org.ambraproject.rhino.model.CitedArticleAuthor;
import org.ambraproject.rhino.model.CitedArticleEditor;

import java.util.List;

public class Citation {

  private String key;
  private Integer year;
  private String displayYear;
  private String month;
  private String day;
  private Integer volumeNumber;
  private String volume;
  private String issue;
  private String title;
  private String publisherLocation;
  private String publisherName;
  private String pages;
  private String eLocationID;
  private String journal;
  private String note;
  private List<String> collaborativeAuthors;
  private String url;
  private String doi;
  private String summary;
  private String citationType;

  private List<NlmPerson> authors;
  private List<NlmPerson> editors;

  public Citation() {
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public Integer getYear() {
    return year;
  }

  public void setYear(Integer year) {
    this.year = year;
  }

  public String getDisplayYear() {
    return displayYear;
  }

  public void setDisplayYear(String displayYear) {
    this.displayYear = displayYear;
  }

  public String getMonth() {
    return month;
  }

  public void setMonth(String month) {
    this.month = month;
  }

  public String getDay() {
    return day;
  }

  public void setDay(String day) {
    this.day = day;
  }

  public Integer getVolumeNumber() {
    return volumeNumber;
  }

  public void setVolumeNumber(Integer volumeNumber) {
    this.volumeNumber = volumeNumber;
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

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
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

  public String getPages() {
    return pages;
  }

  public void setPages(String pages) {
    this.pages = pages;
  }

  public String geteLocationID() {
    return eLocationID;
  }

  public void seteLocationID(String eLocationID) {
    this.eLocationID = eLocationID;
  }

  public String getJournal() {
    return journal;
  }

  public void setJournal(String journal) {
    this.journal = journal;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public List<String> getCollaborativeAuthors() {
    return collaborativeAuthors;
  }

  public void setCollaborativeAuthors(List<String> collaborativeAuthors) {
    this.collaborativeAuthors = collaborativeAuthors;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getDoi() {
    return doi;
  }

  public void setDoi(String doi) {
    this.doi = doi;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public String getCitationType() {
    return citationType;
  }

  public void setCitationType(String citationType) {
    this.citationType = citationType;
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
