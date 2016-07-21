package org.ambraproject.rhino.identity;

import java.util.Objects;

public final class IssueIdentifier {

  private final Doi doi;

  private IssueIdentifier(Doi doi) {
    this.doi = Objects.requireNonNull(doi);
  }

  public static IssueIdentifier create(Doi doi) {
    return new IssueIdentifier(doi);
  }

  public static IssueIdentifier create(String doi) {
    return create(Doi.create(doi));
  }

  public Doi getDoi() {
    return doi;
  }

  @Override
  public String toString() {
    return "issueIdentifier{" +
        "doi=" + doi +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    return this == o || o != null && getClass() == o.getClass() && doi.equals(((IssueIdentifier) o).doi);
  }

  @Override
  public int hashCode() {
    return doi.hashCode();
  }
}
