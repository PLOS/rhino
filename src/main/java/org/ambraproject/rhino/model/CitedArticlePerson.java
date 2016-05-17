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
 * Person associated with an cited article (e.g. author, editor, etc.)
 *
 * @author Alex Kudlick 11/8/11
 */
public abstract class CitedArticlePerson extends AmbraEntity {

  private String fullName;
  private String givenNames;
  private String surnames;
  private String suffix;

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
    if (o == null) return false;
    if (!(o instanceof CitedArticlePerson)) return false;


    CitedArticlePerson article = (CitedArticlePerson) o;

    if (getID() != null ? !getID().equals(article.getID()) : article.getID() != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return getID() != null ? getID().hashCode() : 0;
  }


}
