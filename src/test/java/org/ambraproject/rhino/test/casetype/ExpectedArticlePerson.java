package org.ambraproject.rhino.test.casetype;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.ambraproject.models.ArticlePerson;
import org.ambraproject.rhino.test.IngestionTestCase;
import org.ambraproject.rhino.test.IngestionTestCase.AssertionFailure;
import org.apache.commons.lang.ObjectUtils;

import java.util.Collection;

public class ExpectedArticlePerson extends IngestionTestCase.ExpectedEntity<ArticlePerson> {
  private final String fullName;
  private final String givenNames;
  private final String surnames;
  private final String suffix;

  private ExpectedArticlePerson(Builder builder) {
    super(ArticlePerson.class);
    this.fullName = builder.fullName;
    this.givenNames = builder.givenNames;
    this.surnames = builder.surnames;
    this.suffix = builder.suffix;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public Collection<AssertionFailure> test(ArticlePerson articlePerson) {
    Collection<AssertionFailure> failures = Lists.newArrayList();

    String fullName = articlePerson.getFullName();
    if (!Objects.equal(fullName, this.fullName)) {
      failures.add(AssertionFailure.create(ArticlePerson.class, "fullName", fullName, this.fullName));
    }

    String givenNames = articlePerson.getGivenNames();
    if (!Objects.equal(givenNames, this.givenNames)) {
      failures.add(AssertionFailure.create(ArticlePerson.class, "givenNames", givenNames, this.givenNames));
    }

    String surnames = articlePerson.getSurnames();
    if (!Objects.equal(surnames, this.surnames)) {
      failures.add(AssertionFailure.create(ArticlePerson.class, "surnames", surnames, this.surnames));
    }

    String suffix = articlePerson.getSuffix();
    if (!Objects.equal(suffix, this.suffix)) {
      failures.add(AssertionFailure.create(ArticlePerson.class, "suffix", suffix, this.suffix));
    }

    return ImmutableList.copyOf(failures);
  }

  public static class Builder {
    private String fullName;
    private String givenNames;
    private String surnames;
    private String suffix;

    public Builder setFullName(String fullName) {
      this.fullName = fullName;
      return this;
    }

    public Builder setGivenNames(String givenNames) {
      this.givenNames = givenNames;
      return this;
    }

    public Builder setSurnames(String surnames) {
      this.surnames = surnames;
      return this;
    }

    public Builder setSuffix(String suffix) {
      this.suffix = suffix;
      return this;
    }

    public ExpectedArticlePerson build() {
      return new ExpectedArticlePerson(this);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != getClass()) return false;
    ExpectedArticlePerson that = (ExpectedArticlePerson) obj;
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
