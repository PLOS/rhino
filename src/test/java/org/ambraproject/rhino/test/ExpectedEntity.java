package org.ambraproject.rhino.test;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import org.ambraproject.models.AmbraEntity;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * One entity that is expected to be added to the database as a result of ingesting the article.
 *
 * @param <T> the entity type
 */
public abstract class ExpectedEntity<T extends AmbraEntity> {
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
  public abstract ImmutableCollection<AssertionFailure<?>> test(T actualEntity);

  /**
   * Test a value from one field. If the value is not equal to the expected value, the failure is added to the provided
   * collection.
   *
   * @param failureCollection a mutable collection
   * @param fieldName         the name of the field tested
   * @param actualValue       the field's value from the entity being tested
   * @param expectedValue     the field's expected value
   * @param <V>               the field's type
   * @return {@code true} if the test passed; {@code false} if a failure was added to the collection
   */
  protected <V> boolean testField(Collection<AssertionFailure<?>> failureCollection, String fieldName,
                                  @Nullable V actualValue, @Nullable V expectedValue) {
    if (Objects.equal(actualValue, expectedValue)) {
      return true;
    }
    failureCollection.add(AssertionFailure.create(entityType, fieldName, actualValue, expectedValue));
    return false;
  }

}
