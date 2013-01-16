package org.ambraproject.rhino.test.casetype;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.ambraproject.models.CitedArticlePerson;
import org.ambraproject.rhino.test.AssertionFailure;
import org.ambraproject.rhino.test.ExpectedEntity;
import org.apache.commons.lang.ObjectUtils;

import java.util.Collection;

/**
 * Generated code! See {@code /src/test/python/ingest_test_generation/generate.py}
 */
public class ExpectedCitedArticlePerson extends ExpectedEntity<CitedArticlePerson> {
  private String fullName;
  private String givenNames;
  private String surnames;
  private String suffix;

  public ExpectedCitedArticlePerson() {
    super(CitedArticlePerson.class);
  }

  @Override
  public ImmutableCollection<AssertionFailure<?>> test(CitedArticlePerson citedArticlePerson) {
    Collection<AssertionFailure<?>> failures = Lists.newArrayList();
    testField(failures, "fullName", citedArticlePerson.getFullName(), fullName);
    testField(failures, "givenNames", citedArticlePerson.getGivenNames(), givenNames);
    testField(failures, "surnames", citedArticlePerson.getSurnames(), surnames);
    testField(failures, "suffix", citedArticlePerson.getSuffix(), suffix);
    return ImmutableList.copyOf(failures);
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
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != getClass()) return false;
    ExpectedCitedArticlePerson that = (ExpectedCitedArticlePerson) obj;
    if (!Objects.equal(this.fullName, that.fullName)) return false;
    if (!Objects.equal(this.givenNames, that.givenNames)) return false;
    if (!Objects.equal(this.surnames, that.surnames)) return false;
    if (!Objects.equal(this.suffix, that.suffix)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int hash = 1;
    hash = prime * hash + ObjectUtils.hashCode(fullName);
    hash = prime * hash + ObjectUtils.hashCode(givenNames);
    hash = prime * hash + ObjectUtils.hashCode(surnames);
    hash = prime * hash + ObjectUtils.hashCode(suffix);
    return hash;
  }
}
