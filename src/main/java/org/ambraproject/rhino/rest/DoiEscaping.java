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
   * Indicates that a string, which was expected to be an escaped DOI, contained invalid or ambiguous escaping syntax.
   */
  public static class EscapedDoiException extends RuntimeException {
    private EscapedDoiException(String message) {
      super(message);
    }

    private EscapedDoiException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * Resolve an escaped DOI into its unescaped form.
   *
   * @param escapedDoi a DOI or DOI name encoded into a form for embedding in a URL
   * @return the represented DOI value
   * @throws EscapedDoiException if the input contains an ambiguous escape sequence
   */
  public static Doi resolve(String escapedDoi) {
    int length = escapedDoi.length();
    StringBuilder unescaped = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      char c = escapedDoi.charAt(i);
      if (c == '/') {
        throw new EscapedDoiException("Invalid character: /");
      } else if (c != '+') {
        unescaped.append(c);
      } else {
        char next;
        try {
          next = escapedDoi.charAt(++i);
        } catch (IndexOutOfBoundsException e) {
          throw new EscapedDoiException("Escape sequence begins at end of string", e);
        }

        if (next == '+') {
          unescaped.append('/');
        } else if (next == '-') {
          unescaped.append('+');
        } else {
          throw new EscapedDoiException("Invalid escape: " + next);
        }
      }
    }
    return Doi.create(unescaped.toString());
  }

}
