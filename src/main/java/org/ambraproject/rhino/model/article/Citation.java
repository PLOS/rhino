/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.model.article;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.util.List;

@AutoValue
public abstract class Citation {

  @Nullable public abstract String getKey();
  @Nullable public abstract Integer getYear();
  @Nullable public abstract String getDisplayYear();
  @Nullable public abstract String getMonth();
  @Nullable public abstract String getDay();
  @Nullable public abstract Integer getVolumeNumber();
  @Nullable public abstract String getVolume();
  @Nullable public abstract String getIssue();
  @Nullable public abstract String getTitle();
  @Nullable public abstract String getPublisherLocation();
  @Nullable public abstract String getPublisherName();
  @Nullable public abstract String getPages();
  @Nullable public abstract String geteLocationID();
  @Nullable public abstract String getJournal();
  @Nullable public abstract String getNote();
  @Nullable public abstract List <String> getCollaborativeAuthors();
  @Nullable public abstract String getUrl();
  @Nullable public abstract String getDoi();
  @Nullable public abstract String getSummary();
  @Nullable public abstract String getCitationType();
  @Nullable public abstract ImmutableList<NlmPerson> getAuthors();
  @Nullable public abstract ImmutableList<NlmPerson> getEditors();

  public static Builder builder() { return new AutoValue_Citation.Builder(); }

  @AutoValue.Builder
  abstract static class Builder {
    public abstract Citation build();

    abstract Builder setKey(String key);
    abstract Builder setYear(Integer year);
    abstract Builder setDisplayYear(String displayYear);
    abstract Builder setMonth(String month);
    abstract Builder setDay(String day);
    abstract Builder setVolumeNumber(Integer volumeNumber);
    abstract Builder setVolume(String volume);
    abstract Builder setIssue(String issue);
    abstract Builder setTitle(String title);
    abstract Builder setPublisherLocation(String publisherLocation);
    abstract Builder setPublisherName(String publisherName);
    abstract Builder setPages(String pages);
    abstract Builder seteLocationID(String eLocationID);
    abstract Builder setJournal(String journal);
    abstract Builder setNote(String note);
    abstract Builder setCollaborativeAuthors(List<String> collaborativeAuthors);
    abstract Builder setUrl(String url);
    abstract Builder setDoi(String doi);
    abstract Builder setSummary(String summary);
    abstract Builder setCitationType(String citationType);
    abstract Builder setAuthors(List<NlmPerson> authors);
    abstract Builder setEditors(List<NlmPerson> editors);
  }
}
