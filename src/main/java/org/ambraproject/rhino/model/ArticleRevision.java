package org.ambraproject.rhino.model;

import org.ambraproject.models.AmbraEntity;

public class ArticleRevision extends AmbraEntity {

  private String doi;
  private int revisionNumber;
  private String crepoUuid; // TODO: Configure org.hibernate.type.UUIDCharType so this can be a java.util.UUID

  public String getDoi() {
    return doi;
  }

  public void setDoi(String doi) {
    this.doi = doi;
  }

  public int getRevisionNumber() {
    return revisionNumber;
  }

  public void setRevisionNumber(int revisionNumber) {
    this.revisionNumber = revisionNumber;
  }

  public String getCrepoUuid() {
    return crepoUuid;
  }

  public void setCrepoUuid(String crepoUuid) {
    this.crepoUuid = crepoUuid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleRevision that = (ArticleRevision) o;

    if (revisionNumber != that.revisionNumber) return false;
    if (crepoUuid != null ? !crepoUuid.equals(that.crepoUuid) : that.crepoUuid != null) return false;
    if (doi != null ? !doi.equals(that.doi) : that.doi != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = doi != null ? doi.hashCode() : 0;
    result = 31 * result + revisionNumber;
    result = 31 * result + (crepoUuid != null ? crepoUuid.hashCode() : 0);
    return result;
  }
}
