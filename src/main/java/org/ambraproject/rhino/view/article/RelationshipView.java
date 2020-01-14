/*
 * Copyright (c) 2017-2019 Public Library of Science
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

package org.ambraproject.rhino.view.article;

import java.time.LocalDate;

import javax.annotation.Nullable;

import com.google.auto.value.AutoValue;

import org.ambraproject.rhino.view.journal.JournalOutputView;

@AutoValue
public abstract class RelationshipView {
  public abstract String getDoi();
  @Nullable public abstract LocalDate getPublicationDate();
  @Nullable public abstract Integer getRevisionNumber();
  @Nullable public abstract String getTitle();
  @Nullable public abstract String getSpecificUse();
  public abstract String getType();
  public abstract JournalOutputView getJournal();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract RelationshipView build();
    public abstract Builder setDoi(String doi);
    public abstract Builder setTitle(String title);
    public abstract Builder setType(String type);
    public abstract Builder setJournal(JournalOutputView journal);
    public abstract Builder setRevisionNumber(Integer revisionNumber);
    public abstract Builder setPublicationDate(LocalDate date);
    public abstract Builder setSpecificUse(String specificUse);
  }

  public static Builder builder() {
    return new AutoValue_RelationshipView.Builder();
  }
}
