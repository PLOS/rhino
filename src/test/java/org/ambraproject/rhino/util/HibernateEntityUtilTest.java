package org.ambraproject.rhino.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class HibernateEntityUtilTest {

  // Dummy entity
  private static class KeyValuePair {
    private final String key;
    private String value;

    private KeyValuePair(String key, String value) {
      this.key = Objects.requireNonNull(key);
      this.value = Objects.requireNonNull(value);
    }

    public String getKey() {
      return key;
    }

    public void copyInto(KeyValuePair destination) {
      Preconditions.checkArgument(this.key.equals(destination.key));
      destination.value = Objects.requireNonNull(this.value);
    }

    @Override
    public boolean equals(Object o) {
      return (this == o) || ((o instanceof KeyValuePair)
          && key.equals(((KeyValuePair) o).key) && value.equals(((KeyValuePair) o).value));
    }

    @Override
    public int hashCode() {
      return 31 * key.hashCode() + value.hashCode();
    }
  }

  @Test
  public void testReplacement() {
    KeyValuePair pa = new KeyValuePair("a", "1");
    KeyValuePair pb = new KeyValuePair("b", "2");
    KeyValuePair pc = new KeyValuePair("c", "3");
    KeyValuePair pd = new KeyValuePair("d", "4");

    KeyValuePair ra = new KeyValuePair("a", "5");
    KeyValuePair rb = new KeyValuePair("b", "6");
    KeyValuePair rc = new KeyValuePair("c", "7");
    KeyValuePair re = new KeyValuePair("e", "8");

    List<KeyValuePair> persist = new ArrayList<>(ImmutableList.of(pa, pb, pc, pd));
    List<KeyValuePair> replace = ImmutableList.of(rc, re, ra, rb);

    HibernateEntityUtil.replaceEntities(persist, replace, KeyValuePair::getKey, KeyValuePair::copyInto);

    // Check that persistent identities were preserved (yes, '==' is correct, not '.equals')
    assertTrue(persist.get(0) == pc);
    assertTrue(persist.get(1) == re); // was inserted with new identity
    assertTrue(persist.get(2) == pa);
    assertTrue(persist.get(3) == pb);

    // Check that values were replaced
    assertEquals(persist.size(), replace.size());
    assertEquals(persist.get(0), rc);
    assertEquals(persist.get(2), ra);
    assertEquals(persist.get(3), rb);

    assertFalse(persist.contains(pd)); // was removed
  }

}
