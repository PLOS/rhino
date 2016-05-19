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
 * Person associated with an article (e.g. author, editor, etc.)
 *
 * @author Alex Kudlick 11/8/11
 */
public abstract class ArticlePerson extends AmbraEntity {

  private String fullName;
  private String givenNames;
  private String surnames;
  private String suffix;

  public ArticlePerson() {
    super();
  }

  protected ArticlePerson(String givenNames, String surnames, String suffix) {
    super();
    this.givenNames = givenNames;
    this.surnames = surnames;
    this.suffix = suffix;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getGivenNames() {
    return givenNames;
  }

  public void setGivenNames(String givenNames) {
    this.givenNames = givenNames;
  }

  public String getSurnames() {
    return surnames;
  }

  public void setSurnames(String surnames) {
    this.surnames = surnames;
  }

  public String getSuffix() {
    return suffix;
  }

  public void setSuffix(String suffix) {
    this.suffix = suffix;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ArticlePerson)) return false;

    ArticlePerson that = (ArticlePerson) o;

    if (fullName != null ? !fullName.equals(that.fullName) : that.fullName != null) return false;
    if (givenNames != null ? !givenNames.equals(that.givenNames) : that.givenNames != null) return false;
    if (suffix != null ? !suffix.equals(that.suffix) : that.suffix != null) return false;
    if (surnames != null ? !surnames.equals(that.surnames) : that.surnames != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = fullName != null ? fullName.hashCode() : 0;
    result = 31 * result + (givenNames != null ? givenNames.hashCode() : 0);
    result = 31 * result + (surnames != null ? surnames.hashCode() : 0);
    result = 31 * result + (suffix != null ? suffix.hashCode() : 0);
    return result;
  }
}
