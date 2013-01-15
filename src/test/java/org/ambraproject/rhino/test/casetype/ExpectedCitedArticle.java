package org.ambraproject.rhino.test.casetype;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.ambraproject.models.CitedArticle;
import org.ambraproject.models.CitedArticleAuthor;
import org.ambraproject.models.CitedArticleEditor;
import org.ambraproject.rhino.test.IngestionTestCase;
import org.ambraproject.rhino.test.IngestionTestCase.AssertionFailure;
import org.apache.commons.lang.ObjectUtils;

import java.util.Collection;
import java.util.List;

public class ExpectedCitedArticle extends IngestionTestCase.ExpectedEntity<CitedArticle> {
  private final String key;
  private final Integer year;
  private final String displayYear;
  private final String month;
  private final String day;
  private final Integer volumeNumber;
  private final String volume;
  private final String issue;
  private final String title;
  private final String publisherLocation;
  private final String publisherName;
  private final String pages;
  private final String eLocationID;
  private final String journal;
  private final String note;
  private final List<String> collaborativeAuthors;
  private final String url;
  private final String doi;
  private final String summary;
  private final String citationType;
  private final List<CitedArticleAuthor> authors;
  private final List<CitedArticleEditor> editors;

  private ExpectedCitedArticle(Builder builder) {
    super(CitedArticle.class);
    this.key = builder.key;
    this.year = builder.year;
    this.displayYear = builder.displayYear;
    this.month = builder.month;
    this.day = builder.day;
    this.volumeNumber = builder.volumeNumber;
    this.volume = builder.volume;
    this.issue = builder.issue;
    this.title = builder.title;
    this.publisherLocation = builder.publisherLocation;
    this.publisherName = builder.publisherName;
    this.pages = builder.pages;
    this.eLocationID = builder.eLocationID;
    this.journal = builder.journal;
    this.note = builder.note;
    this.collaborativeAuthors = builder.collaborativeAuthors;
    this.url = builder.url;
    this.doi = builder.doi;
    this.summary = builder.summary;
    this.citationType = builder.citationType;
    this.authors = builder.authors;
    this.editors = builder.editors;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public Collection<AssertionFailure> test(CitedArticle citedArticle) {
    Collection<AssertionFailure> failures = Lists.newArrayList();

    String key = citedArticle.getKey();
    if (!Objects.equal(key, this.key)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "key", key, this.key));
    }

    Integer year = citedArticle.getYear();
    if (!Objects.equal(year, this.year)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "year", year, this.year));
    }

    String displayYear = citedArticle.getDisplayYear();
    if (!Objects.equal(displayYear, this.displayYear)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "displayYear", displayYear, this.displayYear));
    }

    String month = citedArticle.getMonth();
    if (!Objects.equal(month, this.month)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "month", month, this.month));
    }

    String day = citedArticle.getDay();
    if (!Objects.equal(day, this.day)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "day", day, this.day));
    }

    Integer volumeNumber = citedArticle.getVolumeNumber();
    if (!Objects.equal(volumeNumber, this.volumeNumber)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "volumeNumber", volumeNumber, this.volumeNumber));
    }

    String volume = citedArticle.getVolume();
    if (!Objects.equal(volume, this.volume)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "volume", volume, this.volume));
    }

    String issue = citedArticle.getIssue();
    if (!Objects.equal(issue, this.issue)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "issue", issue, this.issue));
    }

    String title = citedArticle.getTitle();
    if (!Objects.equal(title, this.title)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "title", title, this.title));
    }

    String publisherLocation = citedArticle.getPublisherLocation();
    if (!Objects.equal(publisherLocation, this.publisherLocation)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "publisherLocation", publisherLocation, this.publisherLocation));
    }

    String publisherName = citedArticle.getPublisherName();
    if (!Objects.equal(publisherName, this.publisherName)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "publisherName", publisherName, this.publisherName));
    }

    String pages = citedArticle.getPages();
    if (!Objects.equal(pages, this.pages)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "pages", pages, this.pages));
    }

    String eLocationID = citedArticle.geteLocationID();
    if (!Objects.equal(eLocationID, this.eLocationID)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "eLocationID", eLocationID, this.eLocationID));
    }

    String journal = citedArticle.getJournal();
    if (!Objects.equal(journal, this.journal)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "journal", journal, this.journal));
    }

    String note = citedArticle.getNote();
    if (!Objects.equal(note, this.note)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "note", note, this.note));
    }

    List<String> collaborativeAuthors = citedArticle.getCollaborativeAuthors();
    if (!Objects.equal(collaborativeAuthors, this.collaborativeAuthors)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "collaborativeAuthors", collaborativeAuthors, this.collaborativeAuthors));
    }

    String url = citedArticle.getUrl();
    if (!Objects.equal(url, this.url)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "url", url, this.url));
    }

    String doi = citedArticle.getDoi();
    if (!Objects.equal(doi, this.doi)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "doi", doi, this.doi));
    }

    String summary = citedArticle.getSummary();
    if (!Objects.equal(summary, this.summary)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "summary", summary, this.summary));
    }

    String citationType = citedArticle.getCitationType();
    if (!Objects.equal(citationType, this.citationType)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "citationType", citationType, this.citationType));
    }

    List<CitedArticleAuthor> authors = citedArticle.getAuthors();
    if (!Objects.equal(authors, this.authors)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "authors", authors, this.authors));
    }

    List<CitedArticleEditor> editors = citedArticle.getEditors();
    if (!Objects.equal(editors, this.editors)) {
      failures.add(AssertionFailure.create(CitedArticle.class, "editors", editors, this.editors));
    }

    return ImmutableList.copyOf(failures);
  }

  public static class Builder {
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

    public Builder setKey(String key) {
      this.key = key;
      return this;
    }

    public Builder setYear(Integer year) {
      this.year = year;
      return this;
    }

    public Builder setDisplayYear(String displayYear) {
      this.displayYear = displayYear;
      return this;
    }

    public Builder setMonth(String month) {
      this.month = month;
      return this;
    }

    public Builder setDay(String day) {
      this.day = day;
      return this;
    }

    public Builder setVolumeNumber(Integer volumeNumber) {
      this.volumeNumber = volumeNumber;
      return this;
    }

    public Builder setVolume(String volume) {
      this.volume = volume;
      return this;
    }

    public Builder setIssue(String issue) {
      this.issue = issue;
      return this;
    }

    public Builder setTitle(String title) {
      this.title = title;
      return this;
    }

    public Builder setPublisherLocation(String publisherLocation) {
      this.publisherLocation = publisherLocation;
      return this;
    }

    public Builder setPublisherName(String publisherName) {
      this.publisherName = publisherName;
      return this;
    }

    public Builder setPages(String pages) {
      this.pages = pages;
      return this;
    }

    public Builder setELocationID(String eLocationID) {
      this.eLocationID = eLocationID;
      return this;
    }

    public Builder setJournal(String journal) {
      this.journal = journal;
      return this;
    }

    public Builder setNote(String note) {
      this.note = note;
      return this;
    }

    public Builder setCollaborativeAuthors(List<String> collaborativeAuthors) {
      this.collaborativeAuthors = collaborativeAuthors;
      return this;
    }

    public Builder setUrl(String url) {
      this.url = url;
      return this;
    }

    public Builder setDoi(String doi) {
      this.doi = doi;
      return this;
    }

    public Builder setSummary(String summary) {
      this.summary = summary;
      return this;
    }

    public Builder setCitationType(String citationType) {
      this.citationType = citationType;
      return this;
    }

    public Builder setAuthors(List<CitedArticleAuthor> authors) {
      this.authors = authors;
      return this;
    }

    public Builder setEditors(List<CitedArticleEditor> editors) {
      this.editors = editors;
      return this;
    }

    public ExpectedCitedArticle build() {
      return new ExpectedCitedArticle(this);
    }
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
