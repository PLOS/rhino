package org.ambraproject.rhino.test;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.Collection;

import static org.testng.Assert.assertEquals;

/**
 * An object that does multiple "soft" assertions for equality. That is, it does all the assertions and then reports
 * which ones failed, rather than halting on the first failed assertion.
 * <p/>
 * It would be nice to use TestNG features for this, but quick research indicates that it would require custom code
 * anyway.
 */
public class AssertionCollector {

  private final Collection<Success> successes;
  private final Collection<Failure> failures;
  private final boolean failFast;

  /**
   * Constructor.
   *
   * @param failFast if true, the test will throw an assertion error upon the first failed assertion.  If false, all
   *                 failures will be collected and reported at the end of the test.
   */
  public AssertionCollector(boolean failFast) {
    super();
    this.failFast = failFast;
    successes = Lists.newArrayList();
    failures = Lists.newArrayList();
  }

  public AssertionCollector() {
    this(false);
  }

  public int getSuccessCount() {
    return successes.size();
  }

  public ImmutableCollection<Success> getSuccesses() {
    return ImmutableList.copyOf(successes);
  }

  public int getFailureCount() {
    return failures.size();
  }

  public ImmutableCollection<Failure> getFailures() {
    return ImmutableList.copyOf(failures);
  }

  /**
   * Assert that the value from an object field matches an expected value. If they do not match, the failure is recorded
   * in this object.
   *
   * @param objectType the type of the object being tested
   * @param fieldName  the name of the field being tested
   * @param actual     the actual field value
   * @param expected   the expected field value
   * @return {@code true} if the test passed; {@code false} if a failure was recorded
   */
  public boolean compare(Class<?> objectType, String fieldName, @Nullable Object actual, @Nullable Object expected) {
    return compare(objectType.getSimpleName(), fieldName, actual, expected);
  }

  /**
   * Assert that the value from an object field matches an expected value. If they do not match, the failure is recorded
   * in this object.
   *
   * @param objectName the name of the object being tested
   * @param fieldName  the name of the field being tested
   * @param actual     the actual field value
   * @param expected   the expected field value
   * @return {@code true} if the test passed; {@code false} if a failure was recorded
   */
  public boolean compare(String objectName, String fieldName, @Nullable Object actual, @Nullable Object expected) {
    if (Objects.equal(actual, expected)) {
      successes.add(new Success(objectName, fieldName, expected));
      return true;
    }
    Failure failure = new Failure(objectName, fieldName, actual, expected);
    if (failFast) {
      assertEquals(actual, expected, failure.toString());
    } else {
      failures.add(failure);
    }
    return false;
  }

  private static class AssertionCase {
    protected final String objectName;
    protected final String fieldName;

    protected AssertionCase(String objectName, String fieldName) {
      this.objectName = Preconditions.checkNotNull(objectName);
      this.fieldName = Preconditions.checkNotNull(fieldName);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      AssertionCase that = (AssertionCase) o;

      if (!fieldName.equals(that.fieldName)) return false;
      if (!objectName.equals(that.objectName)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = objectName.hashCode();
      result = 31 * result + fieldName.hashCode();
      return result;
    }
  }

  public static class Success extends AssertionCase {
    @Nullable
    private final Object value;

    private Success(String objectName, String fieldName, Object value) {
      super(objectName, fieldName);
      this.value = value;
    }

    @Override
    public String toString() {
      return String.format("[%s.%s: matched {%s}]",
          objectName, fieldName, String.valueOf(value));
    }
  }

  public static class Failure extends AssertionCase {
    @Nullable
    private final Object actual;
    @Nullable
    private final Object expected;

    private Failure(String objectName, String fieldName, @Nullable Object actual, @Nullable Object expected) {
      super(objectName, fieldName);
      this.actual = actual;
      this.expected = expected;
    }

    @Override
    public String toString() {
      return String.format("[%s.%s: expected {%s}; actual {%s}]",
          objectName, fieldName, String.valueOf(expected), String.valueOf(actual));
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;

      Failure failure = (Failure) o;

      if (actual != null ? !actual.equals(failure.actual) : failure.actual != null) return false;
      if (expected != null ? !expected.equals(failure.expected) : failure.expected != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + (actual != null ? actual.hashCode() : 0);
      result = 31 * result + (expected != null ? expected.hashCode() : 0);
      return result;
    }
  }

}
