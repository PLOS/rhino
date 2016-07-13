package org.ambraproject.rhino.rest;

import com.google.common.annotations.VisibleForTesting;
import org.ambraproject.rhino.identity.Doi;

public class DoiEscaping {
  private DoiEscaping() {
    throw new AssertionError("Not instantiable");
  }

  /**
   * Escape a DOI.
   * <p>
   * For now, just a reference implementation. May be needed if our API ever emits service URLs.
   *
   * @param doi a DOI or DOI name
   * @return the DOI in escaped form
   */
  @VisibleForTesting
  static String escape(String doi) {
    return doi.replace("+", "+-").replace("/", "++");
  }

  /**
   * @param escapedDoi
   * @return
   * @throws IllegalArgumentException if the input contains an ambiguous escape sequence
   */
  public static Doi resolve(String escapedDoi) {
    int length = escapedDoi.length();
    StringBuilder unescaped = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      char c = escapedDoi.charAt(i);
      if (c == '/') {
        throw new IllegalArgumentException("Invalid character: /");
      } else if (c != '+') {
        unescaped.append(c);
      } else {
        char next;
        try {
          next = escapedDoi.charAt(++i);
        } catch (IndexOutOfBoundsException e) {
          throw new IllegalArgumentException("Escape sequence begins at end of string", e);
        }

        if (next == '+') {
          unescaped.append('/');
        } else if (next == '-') {
          unescaped.append('+');
        } else {
          throw new IllegalArgumentException("Invalid escape: " + next);
        }
      }
    }
    return Doi.create(unescaped.toString());
  }

}
