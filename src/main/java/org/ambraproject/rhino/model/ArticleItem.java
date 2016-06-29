package org.ambraproject.rhino.model;

import com.google.common.collect.ImmutableMap;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.plos.crepo.model.RepoVersion;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ArticleItem {


  private final DoiBasedIdentity doi;
  private final String type;
  private final ImmutableMap<String, RepoVersion> files;
  private final Optional<Integer> revisionNumber;
  private final PublicationState state;
  private final Instant timestamp;

  public ArticleItem(DoiBasedIdentity doi,
                     String type,
                     Map<String, RepoVersion> files,
                     Integer revisionNumber,
                     PublicationState state,
                     Instant timestamp) {
    this.doi = Objects.requireNonNull(doi);
    this.type = Objects.requireNonNull(type);
    this.files = ImmutableMap.copyOf(files);
    this.revisionNumber = Optional.ofNullable(revisionNumber);
    this.state = Objects.requireNonNull(state);
    this.timestamp = Objects.requireNonNull(timestamp);
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

  public PublicationState getState() {
    return state;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ArticleItem that = (ArticleItem) o;

    if (!doi.equals(that.doi)) return false;
    if (!type.equals(that.type)) return false;
    if (!files.equals(that.files)) return false;
    if (!revisionNumber.equals(that.revisionNumber)) return false;
    return timestamp.equals(that.timestamp);

  }

  @Override
  public int hashCode() {
    int result = doi.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + files.hashCode();
    result = 31 * result + revisionNumber.hashCode();
    result = 31 * result + timestamp.hashCode();
    return result;
  }
}
