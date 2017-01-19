/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

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
   * <p>
   * The substring {@code "++"} represents {@code '/'}, and the substring {@code "+-"} represents a true {@code '+'}
   * character. A {@code '+'} character that is not part of one of these two escape sequences is invalid, as is the
   * {@code '/'} character.
   *
   * @param escapedDoi a DOI or DOI name encoded into a form for embedding in a URL
   * @return the represented DOI value
   * @throws EscapedDoiException if the input contains an ambiguous escape sequence
   */
  public static Doi unescape(String escapedDoi) {
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
