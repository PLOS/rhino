package org.ambraproject.rhino.service.taxonomy;

import java.util.Comparator;
import java.util.Objects;

public final class WeightedTerm {
  private final String term;
  private final int weight;

  public WeightedTerm(String term, int weight) {
    this.term = Objects.requireNonNull(term);
    this.weight = weight;
  }

  public String getTerm() {
    return term;
  }

  public int getWeight() {
    return weight;
  }

  public static final Comparator<WeightedTerm> BY_DESCENDING_WEIGHT =
      Comparator.comparing(WeightedTerm::getWeight).reversed();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    WeightedTerm that = (WeightedTerm) o;

    if (weight != that.weight) return false;
    if (!term.equals(that.term)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = term.hashCode();
    result = 31 * result + weight;
    return result;
  }
}
