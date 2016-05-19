/*
 * Copyright (c) 2006-2012 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
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

package org.ambraproject.rhino.content;

import org.ambraproject.rhino.model.ArticlePerson;
import org.ambraproject.rhino.model.CitedArticlePerson;

/**
 * Immutable holder for a name that can be put into {@link ArticlePerson} or {@link CitedArticlePerson}.
 * <p/>
 * This is a hack to allow code to write to {@link ArticlePerson} and {@link CitedArticlePerson} with minimal
 * duplication, because those two classes lack a superclass or interface to unify them.
 */
public class PersonName {

  private final String fullName;
  private final String givenNames;
  private final String surname;
  private final String suffix;

  public PersonName(String fullName, String givenNames, String surname, String suffix) {
    this.fullName = fullName;
    this.givenNames = givenNames;
    this.surname = surname;
    this.suffix = suffix;
  }


  public <P extends ArticlePerson> P copyTo(P articlePerson) {
    articlePerson.setFullName(fullName);
    articlePerson.setGivenNames(givenNames);
    articlePerson.setSurnames(surname);
    articlePerson.setSuffix(suffix);
    return articlePerson;
  }

  public static PersonName from(ArticlePerson articlePerson) {
    return new PersonName(articlePerson.getFullName(), articlePerson.getGivenNames(),
        articlePerson.getSurnames(), articlePerson.getSuffix());
  }

  public <P extends CitedArticlePerson> P copyTo(P citedArticlePerson) {
    citedArticlePerson.setFullName(fullName);
    citedArticlePerson.setGivenNames(givenNames);
    citedArticlePerson.setSurnames(surname);
    citedArticlePerson.setSuffix(suffix);
    return citedArticlePerson;
  }

  public static PersonName from(CitedArticlePerson citedArticlePerson) {
    return new PersonName(citedArticlePerson.getFullName(), citedArticlePerson.getGivenNames(),
        citedArticlePerson.getSurnames(), citedArticlePerson.getSuffix());
  }


  public String getFullName() {
    return fullName;
  }

  public String getGivenNames() {
    return givenNames;
  }

  public String getSuffix() {
    return suffix;
  }

  public String getSurname() {
    return surname;
  }


  @Override
  public String toString() {
    return fullName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PersonName that = (PersonName) o;

    if (fullName != null ? !fullName.equals(that.fullName) : that.fullName != null) return false;
    if (givenNames != null ? !givenNames.equals(that.givenNames) : that.givenNames != null) return false;
    if (suffix != null ? !suffix.equals(that.suffix) : that.suffix != null) return false;
    if (surname != null ? !surname.equals(that.surname) : that.surname != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = fullName != null ? fullName.hashCode() : 0;
    result = 31 * result + (givenNames != null ? givenNames.hashCode() : 0);
    result = 31 * result + (surname != null ? surname.hashCode() : 0);
    result = 31 * result + (suffix != null ? suffix.hashCode() : 0);
    return result;
  }
}
