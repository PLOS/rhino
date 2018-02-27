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

import com.google.common.collect.ImmutableList;
import com.google.auto.value.AutoValue;

import java.time.LocalDate;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class ArticleMetadata {

  @Nullable public abstract String getDoi();

  @Nullable public abstract String getTitle();
  @Nullable public abstract String getEissn();
  @Nullable public abstract String getJournalName();
  @Nullable public abstract String getDescription();
  @Nullable public abstract String getAbstractText();
  @Nullable public abstract String getRights();
  @Nullable public abstract String getLanguage();
  @Nullable public abstract String getFormat();
  @Nullable public abstract Integer getPageCount();
  @Nullable public abstract String geteLocationId();

  @Nullable public abstract LocalDate getPublicationDate();

  @Nullable public abstract String getVolume();
  @Nullable public abstract String getIssue();

  @Nullable public abstract String getPublisherLocation();
  @Nullable public abstract String getPublisherName();
  @Nullable public abstract String getUrl();

  @Nullable public abstract String getNlmArticleType();
  @Nullable public abstract String getArticleType();

  @Nullable public abstract ImmutableList<AssetMetadata> getAssets();
  @Nullable public abstract ImmutableList<RelatedArticleLink> getRelatedArticles();
  @Nullable public abstract ImmutableList<NlmPerson> getEditors();

  public static Builder builder() {
    return new AutoValue_ArticleMetadata.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract ArticleMetadata build();

    public abstract Builder setDoi(String doi);
    public abstract Builder setTitle(String title);
    public abstract Builder setEissn(String eIssn);
    public abstract Builder setJournalName(String journalName);
    public abstract Builder setDescription(String description);
    public abstract Builder setAbstractText(String abstractText);
    public abstract Builder setRights(String rights);
    public abstract Builder setLanguage(String language);
    public abstract Builder setFormat(String format);
    public abstract Builder setPageCount(Integer pageCount);
    public abstract Builder seteLocationId(String eLocationId);
    public abstract Builder setPublicationDate(LocalDate publicationDate);
    public abstract Builder setVolume(String volume);
    public abstract Builder setIssue(String issue);
    public abstract Builder setPublisherLocation(String publisherLocation);
    public abstract Builder setPublisherName(String publisherName);
    public abstract Builder setUrl(String url);
    public abstract Builder setNlmArticleType(String nlmArticleType);
    public abstract Builder setArticleType(String articleType);
    public abstract Builder setAssets(List<AssetMetadata> assets);
    public abstract Builder setRelatedArticles(List<RelatedArticleLink> relatedArticles);
    public abstract Builder setEditors(List<NlmPerson> editors);
  }
}
