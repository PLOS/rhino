package org.ambraproject.rhino.view.internal;

import org.plos.crepo.model.RepoVersion;

import java.util.Map;

/**
 * Represents a {@link org.plos.crepo.model.RepoVersion} for serializing as JSON.
 */
public class RepoVersionRepr {
  private final String key;
  private final String uuid;

  public RepoVersionRepr(RepoVersion repoVersion) {
    this.key = repoVersion.getKey();
    this.uuid = repoVersion.getUuid().toString();
  }


  public RepoVersion read() {
    return RepoVersion.create(key, uuid);
  }

  public static RepoVersion read(Map<?, ?> deserialized) {
    return RepoVersion.create((String) deserialized.get("key"), (String) deserialized.get("uuid"));
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    RepoVersionRepr that = (RepoVersionRepr) o;
    if (!key.equals(that.key)) return false;
    if (!uuid.equals(that.uuid)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int result = key.hashCode();
    result = 31 * result + uuid.hashCode();
    return result;
  }
}