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

package org.ambraproject.rhino.view.article.author;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.collect.ImmutableList;

/**
 * JSON output view class for authors of an article.
 */
@AutoValue
public abstract class AuthorView {
  @Nullable public abstract String getGivenNames();
  @Nullable public abstract String getSurnames();
  @Nullable public abstract String getSuffix();
  @Nullable public abstract String getOnBehalfOf();
  @Nullable public abstract Orcid getOrcid();
  public abstract boolean getEqualContrib();
  public abstract boolean getDeceased();
  public abstract boolean getRelatedFootnote();
  @Nullable public abstract String getCorresponding();
  public abstract ImmutableList<String> getCurrentAddresses();
  public abstract ImmutableList<String> getAffiliations();
  public abstract ImmutableList<String> getCustomFootnotes();
  public abstract ImmutableList<AuthorRole> getRoles();
  public abstract Builder toBuilder();
  public abstract String getFullName();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setGivenNames(String givenNames);
    public abstract Builder setSurnames(String surnames);
    public abstract Builder setSuffix(String suffix);
    public abstract Builder setOnBehalfOf(String onBehalfOf);
    public abstract Builder setOrcid(Orcid orcid);
    public abstract Builder setEqualContrib(boolean equalContrib);
    public abstract Builder setDeceased(boolean deceased);
    public abstract Builder setRelatedFootnote(boolean relatedFootnote);
    public abstract Builder setCorresponding(String corresponding);
    public abstract Builder setCurrentAddresses(List<String> currentAddresses);
    public abstract Builder setAffiliations(List<String> affiliations);
    public abstract Builder setCustomFootnotes(List<String> customFootnotes);
    public abstract Builder setRoles(List<AuthorRole> roles);
    
    abstract String getSurnames();
    abstract String getSuffix();
    abstract String getGivenNames();
    abstract AuthorView autoBuild();
    abstract Builder setFullName(String fullName);
    public AuthorView build() {
      setFullName(Stream.of(getGivenNames(), getSurnames(), getSuffix()).filter(Objects::nonNull)
          .collect(Collectors.joining(" ")).toString());
      return autoBuild();
    }
  }

  public static Builder builder() {
    return new AutoValue_AuthorView.Builder()
      .setEqualContrib(false)
      .setDeceased(false)
      .setRelatedFootnote(false)
      .setCurrentAddresses(ImmutableList.of())
      .setAffiliations(ImmutableList.of())
      .setCustomFootnotes(ImmutableList.of())
      .setRoles(ImmutableList.of());
  }
}
