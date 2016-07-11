package org.ambraproject.rhino.model.article;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class Citation {

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

  private final ImmutableList<NlmPerson> authors;
  private final ImmutableList<NlmPerson> editors;

  private Citation(Builder builder) {
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
    this.authors = ImmutableList.copyOf(builder.authors);
    this.editors = ImmutableList.copyOf(builder.editors);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Builder() {
    }

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


    public Citation build() {
      return new Citation(this);
    }

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

    public Builder seteLocationID(String eLocationID) {
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

    public Builder setAuthors(List<NlmPerson> authors) {
      this.authors = authors;
      return this;
    }

    public Builder setEditors(List<NlmPerson> editors) {
      this.editors = editors;
      return this;
    }
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Citation citation = (Citation) o;

    if (key != null ? !key.equals(citation.key) : citation.key != null) return false;
    if (year != null ? !year.equals(citation.year) : citation.year != null) return false;
    if (displayYear != null ? !displayYear.equals(citation.displayYear) : citation.displayYear != null) return false;
    if (month != null ? !month.equals(citation.month) : citation.month != null) return false;
    if (day != null ? !day.equals(citation.day) : citation.day != null) return false;
    if (volumeNumber != null ? !volumeNumber.equals(citation.volumeNumber) : citation.volumeNumber != null) {
      return false;
    }
    if (volume != null ? !volume.equals(citation.volume) : citation.volume != null) return false;
    if (issue != null ? !issue.equals(citation.issue) : citation.issue != null) return false;
    if (title != null ? !title.equals(citation.title) : citation.title != null) return false;
    if (publisherLocation != null ? !publisherLocation.equals(citation.publisherLocation) : citation.publisherLocation != null) {
      return false;
    }
    if (publisherName != null ? !publisherName.equals(citation.publisherName) : citation.publisherName != null) {
      return false;
    }
    if (pages != null ? !pages.equals(citation.pages) : citation.pages != null) return false;
    if (eLocationID != null ? !eLocationID.equals(citation.eLocationID) : citation.eLocationID != null) return false;
    if (journal != null ? !journal.equals(citation.journal) : citation.journal != null) return false;
    if (note != null ? !note.equals(citation.note) : citation.note != null) return false;
    if (collaborativeAuthors != null ? !collaborativeAuthors.equals(citation.collaborativeAuthors) : citation.collaborativeAuthors != null) {
      return false;
    }
    if (url != null ? !url.equals(citation.url) : citation.url != null) return false;
    if (doi != null ? !doi.equals(citation.doi) : citation.doi != null) return false;
    if (summary != null ? !summary.equals(citation.summary) : citation.summary != null) return false;
    if (citationType != null ? !citationType.equals(citation.citationType) : citation.citationType != null) {
      return false;
    }
    if (authors != null ? !authors.equals(citation.authors) : citation.authors != null) return false;
    return editors != null ? editors.equals(citation.editors) : citation.editors == null;

  }

  @Override
  public int hashCode() {
    int result = key != null ? key.hashCode() : 0;
    result = 31 * result + (year != null ? year.hashCode() : 0);
    result = 31 * result + (displayYear != null ? displayYear.hashCode() : 0);
    result = 31 * result + (month != null ? month.hashCode() : 0);
    result = 31 * result + (day != null ? day.hashCode() : 0);
    result = 31 * result + (volumeNumber != null ? volumeNumber.hashCode() : 0);
    result = 31 * result + (volume != null ? volume.hashCode() : 0);
    result = 31 * result + (issue != null ? issue.hashCode() : 0);
    result = 31 * result + (title != null ? title.hashCode() : 0);
    result = 31 * result + (publisherLocation != null ? publisherLocation.hashCode() : 0);
    result = 31 * result + (publisherName != null ? publisherName.hashCode() : 0);
    result = 31 * result + (pages != null ? pages.hashCode() : 0);
    result = 31 * result + (eLocationID != null ? eLocationID.hashCode() : 0);
    result = 31 * result + (journal != null ? journal.hashCode() : 0);
    result = 31 * result + (note != null ? note.hashCode() : 0);
    result = 31 * result + (collaborativeAuthors != null ? collaborativeAuthors.hashCode() : 0);
    result = 31 * result + (url != null ? url.hashCode() : 0);
    result = 31 * result + (doi != null ? doi.hashCode() : 0);
    result = 31 * result + (summary != null ? summary.hashCode() : 0);
    result = 31 * result + (citationType != null ? citationType.hashCode() : 0);
    result = 31 * result + (authors != null ? authors.hashCode() : 0);
    result = 31 * result + (editors != null ? editors.hashCode() : 0);
    return result;
  }
}
