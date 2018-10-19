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

package org.ambraproject.rhino.model;

import com.google.auto.value.AutoValue;

import java.sql.Timestamp;
import java.util.Optional;

@AutoValue
abstract public class ArticleFileStorage {
  public static Builder builder() {
    return new AutoValue_ArticleFileStorage.Builder();
  }

  abstract public Optional<String> getContentType();
  abstract public String getCrepoKey();
  abstract public Optional<String> getDownloadName();
  abstract public Long getSize();
  abstract public Timestamp getTimestamp();
  abstract public String getUuid();

  @AutoValue.Builder
  abstract public static class Builder {
    abstract public ArticleFileStorage build();
    abstract public Builder setContentType(Optional<String> value);
    abstract public Builder setCrepoKey(String value);
    abstract public Builder setDownloadName(Optional<String> value);
    abstract public Builder setSize(Long value);
    abstract public Builder setTimestamp(Timestamp value);
    abstract public Builder setUuid(String value);
  }
}
