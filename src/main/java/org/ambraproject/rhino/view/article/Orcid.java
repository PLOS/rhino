package org.ambraproject.rhino.view.article;

import java.util.Objects;

public class Orcid {
  private final String value;
  private final boolean authenticated;

  public Orcid(String value, boolean authenticated) {
    this.value = Objects.requireNonNull(value);
    this.authenticated = authenticated;
  }

  public String getValue() {
    return value;
  }

  public boolean isAuthenticated() {
    return authenticated;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Orcid orcid = (Orcid) o;
    return (authenticated == orcid.authenticated) && value.equals(orcid.value);
  }

  @Override
  public int hashCode() {
    return 31 * value.hashCode() + Boolean.hashCode(authenticated );
  }

}
