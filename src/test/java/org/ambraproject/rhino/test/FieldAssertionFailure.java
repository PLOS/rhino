package org.ambraproject.rhino.test;

import com.google.common.base.Preconditions;

import javax.annotation.Nullable;

/**
 * Defines a mismatch in one field of an expected entity.
 *
 * @param <T> the entity type
 */
public class FieldAssertionFailure<T> {
  private final Class<?> typeChecked;
  private final String fieldChecked;
  @Nullable
  private final T actualValue;
  @Nullable
  private final T expectedValue;

  private FieldAssertionFailure(Class<?> typeChecked, String fieldChecked,
                                @Nullable T actualValue, @Nullable T expectedValue) {
    this.typeChecked = Preconditions.checkNotNull(typeChecked);
    this.fieldChecked = Preconditions.checkNotNull(fieldChecked);
    this.actualValue = actualValue;
    this.expectedValue = expectedValue;
  }

  /**
   * Convenience method, for generic type inference.
   */
  public static <T> FieldAssertionFailure<T> create(Class<?> typeChecked, String fieldChecked,
                                                    @Nullable T actualValue, @Nullable T expectedValue) {
    return new FieldAssertionFailure<T>(typeChecked, fieldChecked, actualValue, expectedValue);
  }

  @Override
  public String toString() {
    return String.format("[%s.%s: expected {%s}; actual {%s}]",
        typeChecked.getSimpleName(), fieldChecked,
        String.valueOf(expectedValue), String.valueOf(actualValue));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    FieldAssertionFailure that = (FieldAssertionFailure) o;

    if (actualValue != null ? !actualValue.equals(that.actualValue) : that.actualValue != null) return false;
    if (expectedValue != null ? !expectedValue.equals(that.expectedValue) : that.expectedValue != null) return false;
    if (!fieldChecked.equals(that.fieldChecked)) return false;
    if (!typeChecked.equals(that.typeChecked)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = typeChecked.hashCode();
    result = 31 * result + fieldChecked.hashCode();
    result = 31 * result + (actualValue != null ? actualValue.hashCode() : 0);
    result = 31 * result + (expectedValue != null ? expectedValue.hashCode() : 0);
    return result;
  }

}
