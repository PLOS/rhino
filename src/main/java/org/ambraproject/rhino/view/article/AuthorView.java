package org.ambraproject.rhino.view.article;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * JSON output view class for authors of an article.
 */
public class AuthorView {

  private final String givenNames;
  private final String surnames;
  private final String fullName;
  private final String suffix;
  private final String onBehalfOf;
  private final boolean equalContrib;
  private final boolean deceased;
  private final boolean relatedFootnote;
  private final String corresponding;
  private final ImmutableList<String> currentAddresses;
  private final ImmutableList<String> affiliations;
  private final ImmutableList<String> customFootnotes;

  private AuthorView(Builder builder) {
    this.givenNames = builder.givenNames;
    this.surnames = builder.surnames;
    this.suffix = builder.suffix;
    this.onBehalfOf = builder.onBehalfOf;
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

  public String getFullName() {
    return fullName;
  }

  private static String buildFullName(String givenNames, String surnames, String suffix) {
    StringBuilder sb = new StringBuilder();

    if (!StringUtils.isEmpty(givenNames)) {
      sb.append(givenNames);
    }

    if (!StringUtils.isEmpty(surnames)) {
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(surnames);
    }

    if (!StringUtils.isEmpty(suffix)) {
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(suffix);
    }

    return sb.toString();
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
    private boolean equalContrib;
    private boolean deceased;
    private boolean relatedFootnote;
    private String corresponding;
    private List<String> currentAddresses;
    private List<String> affiliations;
    private List<String> customFootnotes;

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

    public AuthorView build() {
      return new AuthorView(this);
    }
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AuthorView)) return false;

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
