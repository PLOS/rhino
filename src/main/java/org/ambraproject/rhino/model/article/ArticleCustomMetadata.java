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

import java.time.LocalDate;

public class ArticleCustomMetadata {

  private final LocalDate revisionDate;
  private final String publicationStage;

  private ArticleCustomMetadata(Builder builder) {
    this.revisionDate = builder.revisionDate;
    this.publicationStage = builder.publicationStage;
  }

  public LocalDate getRevisionDate() {
    return revisionDate;
  }

  public String getPublicationStage() {
    return publicationStage;
  }


  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private LocalDate revisionDate;
    private String publicationStage;

    private Builder() {
    }

    public Builder setRevisionDate(LocalDate revisionDate) {
      this.revisionDate = revisionDate;
      return this;
    }

    public Builder setPublicationStage(String publicationStage) {
      this.publicationStage = publicationStage;
      return this;
    }

    public ArticleCustomMetadata build() {
      return new ArticleCustomMetadata(this);
    }
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleCustomMetadata that = (ArticleCustomMetadata) o;

    if (revisionDate != null ? !revisionDate.equals(that.revisionDate) : that.revisionDate != null) return false;
    return publicationStage != null ? publicationStage.equals(that.publicationStage) : that.publicationStage == null;

  }

  @Override
  public int hashCode() {
    int result = revisionDate != null ? revisionDate.hashCode() : 0;
    result = 31 * result + (publicationStage != null ? publicationStage.hashCode() : 0);
    return result;
  }
}
