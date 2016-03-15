package org.ambraproject.rhino.service.taxonomy;

import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;

import java.util.Objects;

public final class WeightedTerm {
  private final String path;
  private final int weight;

  public WeightedTerm(String path, int weight) {
    this.path = Objects.requireNonNull(path);
    this.weight = weight;
  }

  public String getPath() {
    return path;
  }

  public int getWeight() {
    return weight;
  }


  private static final Splitter TERM_SPLITTER = Splitter.on('/');

  public String getLeafTerm() {
    return Iterables.getLast(TERM_SPLITTER.split(path));
  }

  public static final Ordering<WeightedTerm> BY_DESCENDING_WEIGHT =
      Ordering.natural().onResultOf(WeightedTerm::getWeight).reverse();


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    WeightedTerm that = (WeightedTerm) o;

    if (weight != that.weight) return false;
    if (!path.equals(that.path)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = path.hashCode();
    result = 31 * result + weight;
    return result;
  }

  @Override
  public String toString() {
    return "WeightedTerm{" +
        "path='" + path + '\'' +
        ", weight=" + weight +
        '}';
  }
}