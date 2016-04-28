package org.ambraproject.rhino.model;

import com.google.common.collect.ImmutableMap;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.plos.crepo.model.RepoVersion;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ScholarlyWork {

  private final DoiBasedIdentity doi;
  private final String type;
  private final ImmutableMap<String, RepoVersion> files;
  private final Optional<Integer> revisionNumber;

  public ScholarlyWork(DoiBasedIdentity doi, String type, Map<String, RepoVersion> files, Integer revisionNumber) {
    this.doi = Objects.requireNonNull(doi);
    this.type = Objects.requireNonNull(type);
    this.files = ImmutableMap.copyOf(files);
    this.revisionNumber = Optional.ofNullable(revisionNumber);
  }

  public DoiBasedIdentity getDoi() {
    return doi;
  }

  public String getType() {
    return type;
  }

  public Optional<RepoVersion> getFile(String fileType) {
    return Optional.ofNullable(files.get(fileType));
  }

  public Optional<Integer> getRevisionNumber() {
    return revisionNumber;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ScholarlyWork that = (ScholarlyWork) o;

    if (!doi.equals(that.doi)) return false;
    if (!files.equals(that.files)) return false;
    if (!revisionNumber.equals(that.revisionNumber)) return false;
    if (!type.equals(that.type)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = doi.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + files.hashCode();
    result = 31 * result + revisionNumber.hashCode();
    return result;
  }
}
