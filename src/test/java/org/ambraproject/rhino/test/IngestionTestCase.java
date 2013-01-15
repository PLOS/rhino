package org.ambraproject.rhino.test;

import com.google.common.base.Preconditions;
import org.ambraproject.models.AmbraEntity;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * A set of entities expected to be created as the result of ingesting one article.
 * <p/>
 * TODO: Finish implementing. This defines the framework for the test cases but does not use any of the classes. A
 * complete ingestion test case will contain a collection of {@code ExpectedEntity} objects.
 */
public class IngestionTestCase {

  /**
   * Defines a mismatch in one field of an expected entity.
   *
   * @param <T> the entity type
   */
  public static class AssertionFailure<T> {
    private final Class<?> typeChecked;
    private final String fieldChecked;
    @Nullable
    private final T actualValue;
    @Nullable
    private final T expectedValue;

    private AssertionFailure(Class<?> typeChecked, String fieldChecked,
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

  /**
   * One entity that is expected to be added to the database as a result of ingesting the article.
   *
   * @param <T> the entity type
   */
  public static abstract class ExpectedEntity<T extends AmbraEntity> {
    private final Class<T> entityType;

    protected ExpectedEntity(Class<T> entityType) {
      this.entityType = Preconditions.checkNotNull(entityType);
    }

    /**
     * Check this object's expected values against an actual entity that was created while executing a test case.
     * <p/>
     * The test was passed if the return value is empty (size 0). The return value is never null.
     *
     * @param actualEntity the created entity
     * @return the set of failed assertions about the entity's fields
     */
    public abstract Collection<AssertionFailure> test(T actualEntity);
  }

}
