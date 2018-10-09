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

package org.ambraproject.rhino.model.ingest;

import org.plos.crepo.model.input.RepoObjectInput;
import com.google.auto.value.AutoValue;
import java.util.Objects;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.util.Archive;

@AutoValue
abstract public class ArticleFileInput {
  public static Builder builder() {
    return new AutoValue_ArticleFileInput.Builder();
  }

  abstract public Archive getArchive();
  abstract public String getContentType();
  abstract public String getDestinationBucketName();
  abstract public String getDownloadName();
  abstract public ManifestXml.ManifestFile getManifestFile();

  public String getFilename() {
    return this.getManifestFile().getEntry();
  }

  @AutoValue.Builder
  abstract public static class Builder {
    abstract public ArticleFileInput build();
    abstract public Builder setArchive(Archive value);
    abstract public Builder setContentType(String value);
    abstract public Builder setDestinationBucketName(String value);
    abstract public Builder setDownloadName(String value);
    abstract public Builder setManifestFile(ManifestXml.ManifestFile value);
  }
}
