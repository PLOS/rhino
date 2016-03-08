package org.ambraproject.rhino.service.taxonomy;

import org.ambraproject.models.Category;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

public final class WeightedCategory {
  private final Category category;
  private final int weight;

  public WeightedCategory(Category category, int weight) {
    this.category = Objects.requireNonNull(category);
    this.weight = weight;
  }

  public Category getCategory() {
    return category;
  }

  public int getWeight() {
    return weight;
  }


  private static final BinaryOperator<Integer> DUPLICATE_CATEGORY_MERGER = (a, b) -> {
    throw new IllegalArgumentException("Duplicate categories");
  };

  public static Map<Category, Integer> toMap(Collection<WeightedCategory> categories) {
    return categories.stream()
        .collect(Collectors.toMap(WeightedCategory::getCategory, WeightedCategory::getWeight,
            DUPLICATE_CATEGORY_MERGER, LinkedHashMap::new));
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    WeightedCategory that = (WeightedCategory) o;

    if (weight != that.weight) return false;
    if (!category.equals(that.category)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = category.hashCode();
    result = 31 * result + weight;
    return result;
  }
}
