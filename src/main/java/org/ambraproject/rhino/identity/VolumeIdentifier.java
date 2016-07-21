package org.ambraproject.rhino.identity;

import java.util.Objects;

public final class VolumeIdentifier {

  private final Doi doi;

  private VolumeIdentifier(Doi doi) {
    this.doi = Objects.requireNonNull(doi);
  }

  public static VolumeIdentifier create(Doi doi) {
    return new VolumeIdentifier(doi);
  }

  public static VolumeIdentifier create(String doi) {
    return create(Doi.create(doi));
  }

  public Doi getDoi() {
    return doi;
  }

  @Override
  public String toString() {
    return "volumeIdentifier{" +
        "doi=" + doi +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    return this == o || o != null && getClass() == o.getClass() && doi.equals(((VolumeIdentifier) o).doi);
  }

  @Override
  public int hashCode() {
    return doi.hashCode();
  }
}
