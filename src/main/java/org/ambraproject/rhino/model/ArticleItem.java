package org.ambraproject.rhino.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.plos.crepo.model.identity.RepoVersion;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ArticleItem {

  public static enum Visibility {
    /**
     * The article version has been ingested, but is not yet visible to end users.
     */
    INGESTED(0),
    /**
     * The article version is visible to end users.
     */
    PUBLISHED(1),
    /**
     * The article version was visible to end users, but has been taken down.
     */
    DISABLED(2),
    /**
     * The article version has been replaced by a more recently ingested version with the same revision number.
     */
    REPLACED(3);

    private final String label;
    private final int value;

    private Visibility(int value) {
      this.label = name().toLowerCase();
      this.value = value;
    }

    public String getLabel() {
      return label;
    }

    public int getValue() {
      return value;
    }

    private static final ImmutableMap<Integer, Visibility> BY_VALUE = Maps.uniqueIndex(
        EnumSet.allOf(Visibility.class), Visibility::getValue);

    public static Visibility fromValue(int value) {
      Visibility state = BY_VALUE.get(value);
      if (state == null) {
        throw new IllegalArgumentException(
            String.format("Received value: %d. Must be one of: %s", value, BY_VALUE.keySet()));
      }
      return state;
    }
  }


  private final DoiBasedIdentity doi;
  private final String type;
  private final ImmutableMap<String, RepoVersion> files;
  private final Optional<Integer> revisionNumber;
  private final Visibility state;
  private final Instant timestamp;

  public ArticleItem(DoiBasedIdentity doi,
                     String type,
                     Map<String, RepoVersion> files,
                     Integer revisionNumber,
                     Visibility state,
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

  public Visibility getState() {
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
