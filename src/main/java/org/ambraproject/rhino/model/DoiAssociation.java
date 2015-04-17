package org.ambraproject.rhino.model;

import org.ambraproject.models.AmbraEntity;

public class DoiAssociation extends AmbraEntity {

  private String doi;
  private String parentArticleDoi;

  public String getDoi() {
    return doi;
  }

  public void setDoi(String doi) {
    this.doi = doi;
  }

  public String getParentArticleDoi() {
    return parentArticleDoi;
  }

  public void setParentArticleDoi(String parentArticleDoi) {
    this.parentArticleDoi = parentArticleDoi;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DoiAssociation that = (DoiAssociation) o;

    if (doi != null ? !doi.equals(that.doi) : that.doi != null) return false;
    if (parentArticleDoi != null ? !parentArticleDoi.equals(that.parentArticleDoi) : that.parentArticleDoi != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = doi != null ? doi.hashCode() : 0;
    result = 31 * result + (parentArticleDoi != null ? parentArticleDoi.hashCode() : 0);
    return result;
  }

}
