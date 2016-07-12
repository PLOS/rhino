package org.ambraproject.rhino.view.article.author;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * JSON output view class for authors of an article.
 */
public class AuthorView {

  private final String givenNames;
  private final String surnames;
  private final String fullName;
  private final String suffix;
  private final String onBehalfOf;
  private final Orcid orcid;
  private final boolean equalContrib;
  private final boolean deceased;
  private final boolean relatedFootnote;
  private final String corresponding;
  private final ImmutableList<String> currentAddresses;
  private final ImmutableList<String> affiliations;
  private final ImmutableList<String> customFootnotes;
  private final ImmutableList<AuthorRole> roles;

  private AuthorView(Builder builder) {
    this.givenNames = Strings.emptyToNull(builder.givenNames);
    this.surnames = Strings.emptyToNull(builder.surnames);
    this.suffix = Strings.emptyToNull(builder.suffix);
    this.onBehalfOf = Strings.emptyToNull(builder.onBehalfOf);
    this.orcid = builder.orcid;
    this.equalContrib = builder.equalContrib;
    this.deceased = builder.deceased;
    this.relatedFootnote = builder.relatedFootnote;
    this.corresponding = builder.corresponding;
    this.currentAddresses = (builder.currentAddresses == null)
        ? ImmutableList.of()
        : ImmutableList.copyOf(builder.currentAddresses);
    this.affiliations = (builder.affiliations == null)
        ? ImmutableList.of()
        : ImmutableList.copyOf(builder.affiliations);
    this.customFootnotes = (builder.customFootnotes == null)
        ? ImmutableList.of()
        : ImmutableList.copyOf(builder.customFootnotes);
    this.roles = (builder.roles == null)
        ? ImmutableList.of()
        : ImmutableList.copyOf(builder.roles);

    this.fullName = buildFullName(givenNames, surnames, suffix);
  }

  public String getGivenNames() {
    return givenNames;
  }

  public String getSurnames() {
    return surnames;
  }

  public String getSuffix() {
    return suffix;
  }

  public String getOnBehalfOf() {
    return onBehalfOf;
  }

  public Orcid getOrcid() {
    return orcid;
  }

  public boolean getEqualContrib() {
    return equalContrib;
  }

  public boolean getDeceased() {
    return deceased;
  }

  public boolean getRelatedFootnote() {
    return relatedFootnote;
  }

  public String getCorresponding() {
    return corresponding;
  }

  public ImmutableList<String> getCurrentAddresses() {
    return currentAddresses;
  }

  public ImmutableList<String> getAffiliations() {
    return affiliations;
  }

  public ImmutableList<String> getCustomFootnotes() {
    return customFootnotes;
  }

  public ImmutableList<AuthorRole> getRoles() {
    return roles;
  }

  public String getFullName() {
    return fullName;
  }

  private static String buildFullName(String givenNames, String surnames, String suffix) {
    return Stream.of(givenNames, surnames, suffix)
        .filter(Objects::nonNull)
        .collect(Collectors.joining(" "));
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Create a builder set to create a copy of the passed in view
   *
   * @param av
   * @return
   */
  public static Builder builder(AuthorView av) {
    Builder builder = new Builder();

    builder.setGivenNames(av.getGivenNames());
    builder.setSurnames(av.getSurnames());
    builder.setSuffix(av.getSuffix());
    builder.setOnBehalfOf(av.getOnBehalfOf());
    builder.setOrcid(av.getOrcid());
    builder.setEqualContrib(av.getEqualContrib());
    builder.setDeceased(av.getDeceased());
    builder.setCorresponding(av.getCorresponding());
    builder.setCurrentAddresses(av.getCurrentAddresses());
    builder.setAffiliations(av.getAffiliations());
    builder.setCustomFootnotes(av.getCustomFootnotes());

    return builder;
  }

  public static class Builder {
    private Builder() {
    }

    private String givenNames;
    private String surnames;
    private String suffix;
    private String onBehalfOf;
    private Orcid orcid;
    private boolean equalContrib;
    private boolean deceased;
    private boolean relatedFootnote;
    private String corresponding;
    private List<String> currentAddresses;
    private List<String> affiliations;
    private List<String> customFootnotes;
    private List<AuthorRole> roles;

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

    public Builder setOnBehalfOf(String onBehalfOf) {
      this.onBehalfOf = onBehalfOf;
      return this;
    }

    public Builder setOrcid(Orcid orcid) {
      this.orcid = orcid;
      return this;
    }

    public Builder setEqualContrib(boolean equalContrib) {
      this.equalContrib = equalContrib;
      return this;
    }

    public Builder setDeceased(boolean deceased) {
      this.deceased = deceased;
      return this;
    }

    public Builder setRelatedFootnote(boolean relatedFootnote) {
      this.relatedFootnote = relatedFootnote;
      return this;
    }

    public Builder setCorresponding(String corresponding) {
      this.corresponding = corresponding;
      return this;
    }

    public Builder setCurrentAddresses(List<String> currentAddresses) {
      this.currentAddresses = currentAddresses;
      return this;
    }

    public Builder setAffiliations(List<String> affiliations) {
      this.affiliations = affiliations;
      return this;
    }

    public Builder setCustomFootnotes(List<String> customFootnotes) {
      this.customFootnotes = customFootnotes;
      return this;
    }

    public Builder setRoles(List<AuthorRole> roles) {
      this.roles = roles;
      return this;
    }

    public AuthorView build() {
      return new AuthorView(this);
    }
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AuthorView that = (AuthorView) o;

    if (deceased != that.deceased) return false;
    if (equalContrib != that.equalContrib) return false;
    if (relatedFootnote != that.relatedFootnote) return false;
    if (affiliations != null ? !affiliations.equals(that.affiliations) : that.affiliations != null) return false;
    if (corresponding != null ? !corresponding.equals(that.corresponding) : that.corresponding != null) return false;
    if (currentAddresses != null ? !currentAddresses.equals(that.currentAddresses) : that.currentAddresses != null) {
      return false;
    }
    if (customFootnotes != null ? !customFootnotes.equals(that.customFootnotes) : that.customFootnotes != null) {
      return false;
    }
    if (fullName != null ? !fullName.equals(that.fullName) : that.fullName != null) return false;
    if (givenNames != null ? !givenNames.equals(that.givenNames) : that.givenNames != null) return false;
    if (onBehalfOf != null ? !onBehalfOf.equals(that.onBehalfOf) : that.onBehalfOf != null) return false;
    if (orcid != null ? !orcid.equals(that.orcid) : that.orcid != null) return false;
    if (suffix != null ? !suffix.equals(that.suffix) : that.suffix != null) return false;
    if (surnames != null ? !surnames.equals(that.surnames) : that.surnames != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = givenNames != null ? givenNames.hashCode() : 0;
    result = 31 * result + (surnames != null ? surnames.hashCode() : 0);
    result = 31 * result + (fullName != null ? fullName.hashCode() : 0);
    result = 31 * result + (suffix != null ? suffix.hashCode() : 0);
    result = 31 * result + (onBehalfOf != null ? onBehalfOf.hashCode() : 0);
    result = 31 * result + (orcid != null ? orcid.hashCode() : 0);
    result = 31 * result + (equalContrib ? 1 : 0);
    result = 31 * result + (deceased ? 1 : 0);
    result = 31 * result + (relatedFootnote ? 1 : 0);
    result = 31 * result + (corresponding != null ? corresponding.hashCode() : 0);
    result = 31 * result + (currentAddresses != null ? currentAddresses.hashCode() : 0);
    result = 31 * result + (affiliations != null ? affiliations.hashCode() : 0);
    result = 31 * result + (customFootnotes != null ? customFootnotes.hashCode() : 0);
    return result;
  }
}
