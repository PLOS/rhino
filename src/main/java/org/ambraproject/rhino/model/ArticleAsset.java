/*
 * $HeadURL$
 * $Id$
 *
 * Copyright (c) 2006-2011 by Public Library of Science
 *     http://plos.org
 *     http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.model;

/**
 * An asset associated with an article (e.g. a file)
 *
 * @author Alex Kudlick 11/8/11
 */
public class ArticleAsset extends AmbraEntity {
  private String doi;
  private String contextElement;
  private String extension;
  private String contentType;
  private String title;
  private String description;
  private long size;

  public ArticleAsset() {
    super();
  }

  public ArticleAsset(String doi, String extension) {
    this();
    this.doi = doi;
    this.extension = extension;
  }

  public String getDoi() {
    return doi;
  }

  public void setDoi(String doi) {
    this.doi = doi;
  }

  public String getContextElement() {
    return contextElement;
  }

  public void setContextElement(String contextElement) {
    this.contextElement = contextElement;
  }

  public String getExtension() {
    return extension;
  }

  public void setExtension(String extension) {
    this.extension = extension;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ArticleAsset)) return false;

    ArticleAsset that = (ArticleAsset) o;

    if (getID() != null ? !getID().equals(that.getID()) : that.getID() != null) return false;
    if (doi != null ? !doi.equals(that.doi) : that.doi != null) return false;
    if (extension != null ? !extension.equals(that.extension) : that.extension != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = doi != null ? doi.hashCode() : 0;
    result = 31 * result + (getID() != null ? getID().hashCode() : 0);
    result = 31 * result + (extension != null ? extension.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "ArticleAsset{" +
        "doi='" + doi + '\'' +
        ", extension='" + extension + '\'' +
        '}';
  }
}
