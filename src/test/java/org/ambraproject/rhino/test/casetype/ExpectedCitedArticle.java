package org.ambraproject.rhino.test.casetype;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.ambraproject.models.CitedArticle;
import org.ambraproject.models.CitedArticleAuthor;
import org.ambraproject.models.CitedArticleEditor;
import org.ambraproject.rhino.test.AssertionFailure;
import org.ambraproject.rhino.test.ExpectedEntity;
import org.apache.commons.lang.ObjectUtils;

import java.util.Collection;
import java.util.List;

/**
 * Generated code! See {@code /src/test/python/ingest_test_generation/generate.py}
 */
public class ExpectedCitedArticle extends ExpectedEntity<CitedArticle> {
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
  private List<CitedArticleAuthor> authors;
  private List<CitedArticleEditor> editors;

  public ExpectedCitedArticle() {
    super(CitedArticle.class);
  }

  @Override
  public ImmutableCollection<AssertionFailure<?>> test(CitedArticle citedArticle) {
    Collection<AssertionFailure<?>> failures = Lists.newArrayList();
    testField(failures, "key", citedArticle.getKey(), key);
    testField(failures, "year", citedArticle.getYear(), year);
    testField(failures, "displayYear", citedArticle.getDisplayYear(), displayYear);
    testField(failures, "month", citedArticle.getMonth(), month);
    testField(failures, "day", citedArticle.getDay(), day);
    testField(failures, "volumeNumber", citedArticle.getVolumeNumber(), volumeNumber);
    testField(failures, "volume", citedArticle.getVolume(), volume);
    testField(failures, "issue", citedArticle.getIssue(), issue);
    testField(failures, "title", citedArticle.getTitle(), title);
    testField(failures, "publisherLocation", citedArticle.getPublisherLocation(), publisherLocation);
    testField(failures, "publisherName", citedArticle.getPublisherName(), publisherName);
    testField(failures, "pages", citedArticle.getPages(), pages);
    testField(failures, "eLocationID", citedArticle.geteLocationID(), eLocationID);
    testField(failures, "journal", citedArticle.getJournal(), journal);
    testField(failures, "note", citedArticle.getNote(), note);
    testField(failures, "collaborativeAuthors", citedArticle.getCollaborativeAuthors(), collaborativeAuthors);
    testField(failures, "url", citedArticle.getUrl(), url);
    testField(failures, "doi", citedArticle.getDoi(), doi);
    testField(failures, "summary", citedArticle.getSummary(), summary);
    testField(failures, "citationType", citedArticle.getCitationType(), citationType);
    testField(failures, "authors", citedArticle.getAuthors(), authors);
    testField(failures, "editors", citedArticle.getEditors(), editors);
    return ImmutableList.copyOf(failures);
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

  public String getELocationID() {
    return eLocationID;
  }

  public void setELocationID(String eLocationID) {
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

  public List<CitedArticleAuthor> getAuthors() {
    return authors;
  }

  public void setAuthors(List<CitedArticleAuthor> authors) {
    this.authors = authors;
  }

  public List<CitedArticleEditor> getEditors() {
    return editors;
  }

  public void setEditors(List<CitedArticleEditor> editors) {
    this.editors = editors;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != getClass()) return false;
    ExpectedCitedArticle that = (ExpectedCitedArticle) obj;
    if (!Objects.equal(this.key, that.key)) return false;
    if (!Objects.equal(this.year, that.year)) return false;
    if (!Objects.equal(this.displayYear, that.displayYear)) return false;
    if (!Objects.equal(this.month, that.month)) return false;
    if (!Objects.equal(this.day, that.day)) return false;
    if (!Objects.equal(this.volumeNumber, that.volumeNumber)) return false;
    if (!Objects.equal(this.volume, that.volume)) return false;
    if (!Objects.equal(this.issue, that.issue)) return false;
    if (!Objects.equal(this.title, that.title)) return false;
    if (!Objects.equal(this.publisherLocation, that.publisherLocation)) return false;
    if (!Objects.equal(this.publisherName, that.publisherName)) return false;
    if (!Objects.equal(this.pages, that.pages)) return false;
    if (!Objects.equal(this.eLocationID, that.eLocationID)) return false;
    if (!Objects.equal(this.journal, that.journal)) return false;
    if (!Objects.equal(this.note, that.note)) return false;
    if (!Objects.equal(this.collaborativeAuthors, that.collaborativeAuthors)) return false;
    if (!Objects.equal(this.url, that.url)) return false;
    if (!Objects.equal(this.doi, that.doi)) return false;
    if (!Objects.equal(this.summary, that.summary)) return false;
    if (!Objects.equal(this.citationType, that.citationType)) return false;
    if (!Objects.equal(this.authors, that.authors)) return false;
    if (!Objects.equal(this.editors, that.editors)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int hash = 1;
    hash = prime * hash + ObjectUtils.hashCode(key);
    hash = prime * hash + ObjectUtils.hashCode(year);
    hash = prime * hash + ObjectUtils.hashCode(displayYear);
    hash = prime * hash + ObjectUtils.hashCode(month);
    hash = prime * hash + ObjectUtils.hashCode(day);
    hash = prime * hash + ObjectUtils.hashCode(volumeNumber);
    hash = prime * hash + ObjectUtils.hashCode(volume);
    hash = prime * hash + ObjectUtils.hashCode(issue);
    hash = prime * hash + ObjectUtils.hashCode(title);
    hash = prime * hash + ObjectUtils.hashCode(publisherLocation);
    hash = prime * hash + ObjectUtils.hashCode(publisherName);
    hash = prime * hash + ObjectUtils.hashCode(pages);
    hash = prime * hash + ObjectUtils.hashCode(eLocationID);
    hash = prime * hash + ObjectUtils.hashCode(journal);
    hash = prime * hash + ObjectUtils.hashCode(note);
    hash = prime * hash + ObjectUtils.hashCode(collaborativeAuthors);
    hash = prime * hash + ObjectUtils.hashCode(url);
    hash = prime * hash + ObjectUtils.hashCode(doi);
    hash = prime * hash + ObjectUtils.hashCode(summary);
    hash = prime * hash + ObjectUtils.hashCode(citationType);
    hash = prime * hash + ObjectUtils.hashCode(authors);
    hash = prime * hash + ObjectUtils.hashCode(editors);
    return hash;
  }
}
