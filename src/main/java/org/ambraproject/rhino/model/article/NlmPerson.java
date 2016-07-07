package org.ambraproject.rhino.model.article;

import org.ambraproject.rhino.content.PersonName;
import org.ambraproject.rhino.model.ArticlePerson;
import org.ambraproject.rhino.model.CitedArticlePerson;

public class NlmPerson {

  private final String fullName;
  private final String givenNames;
  private final String surname;
  private final String suffix;

  public NlmPerson(String fullName, String givenNames, String surname, String suffix) {
    this.fullName = fullName;
    this.givenNames = givenNames;
    this.surname = surname;
    this.suffix = suffix;
  }


//  public <P extends ArticlePerson> P copyTo(P articlePerson) {
//    articlePerson.setFullName(fullName);
//    articlePerson.setGivenNames(givenNames);
//    articlePerson.setSurnames(surname);
//    articlePerson.setSuffix(suffix);
//    return articlePerson;
//  }
//
//  public static PersonName from(ArticlePerson articlePerson) {
//    return new PersonName(articlePerson.getFullName(), articlePerson.getGivenNames(),
//        articlePerson.getSurnames(), articlePerson.getSuffix());
//  }
//
//  public <P extends CitedArticlePerson> P copyTo(P citedArticlePerson) {
//    citedArticlePerson.setFullName(fullName);
//    citedArticlePerson.setGivenNames(givenNames);
//    citedArticlePerson.setSurnames(surname);
//    citedArticlePerson.setSuffix(suffix);
//    return citedArticlePerson;
//  }
//
//  public static PersonName from(CitedArticlePerson citedArticlePerson) {
//    return new PersonName(citedArticlePerson.getFullName(), citedArticlePerson.getGivenNames(),
//        citedArticlePerson.getSurnames(), citedArticlePerson.getSuffix());
//  }


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

    NlmPerson that = (NlmPerson) o;

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
