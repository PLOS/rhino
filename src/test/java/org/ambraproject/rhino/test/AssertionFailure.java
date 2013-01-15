package org.ambraproject.rhino.test;

import com.google.common.base.Preconditions;

import javax.annotation.Nullable;

/**
 * Defines a mismatch in one field of an expected entity.
 *
 * @param <T> the entity type
 */
public class AssertionFailure<T> {
  private final Class<?> typeChecked;
  private final String fieldChecked;
  @Nullable
  private final T actualValue;
  @Nullable
  private final T expectedValue;

  AssertionFailure(Class<?> typeChecked, String fieldChecked,
                   @Nullable T actualValue, @Nullable T expectedValue) {
    this.typeChecked = Preconditions.checkNotNull(typeChecked);
    this.fieldChecked = Preconditions.checkNotNull(fieldChecked);
    this.actualValue = actualValue;
    this.expectedValue = expectedValue;
  }

  /**
   * Convenience method, for generic type inference.
   */
  public static <T> AssertionFailure<T> create(Class<?> typeChecked, String fieldChecked,
                                               @Nullable T actualValue, @Nullable T expectedValue) {
    return new AssertionFailure<T>(typeChecked, fieldChecked, actualValue, expectedValue);
  }

  @Override
  public String toString() {
    return String.format("[%s.%s: expected {%s}; actual {%s}]",
        typeChecked.getSimpleName(), fieldChecked,
        String.valueOf(expectedValue), String.valueOf(actualValue));
  }

}
