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

package org.ambraproject.rhino.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for dealing with PLOS-specific DOIs.
 */
public final class PlosDoiUtils {

  // TODO: this will probably have to be modified/expanded as we complete the code.
  private static final Pattern PLOS_JOURNAL_DOI_RE = Pattern.compile(
      "(info:doi\\/)?10\\.1371\\/journal\\.p[a-z]{3}\\.\\d{7}(\\.[a-z]\\d+)?");
  private static final Pattern PLOS_VOLUME_DOI_RE = Pattern.compile(
      "(info:doi\\/)?10\\.1371\\/volume\\.p[a-z]{3}\\.v\\d+");
  private static final Pattern[] PLOS_DOI_RES = new Pattern[]{
      PLOS_JOURNAL_DOI_RE,
      PLOS_VOLUME_DOI_RE,
  };
  private static final Pattern PLOS_ARCHIVE_FILE_RE = Pattern.compile("p[a-z]{3}\\.\\d{7}\\.zip");

  private PlosDoiUtils() {
  }

  /**
   * Validates that the input is a well-formed PLOS DOI.
   */
  public static boolean validate(String doi) {
    if (doi == null) {
      return false;
    }
    for (Pattern pattern : PLOS_DOI_RES) {
      Matcher m = pattern.matcher(doi);
      if (m.matches()) {
        return true;
      }
    }
    return false;
  }

  private static final Pattern SHORT_IDENTIFIER_RE = Pattern.compile("p[a-z]{3}\\.\\d{7}");

  /**
   * Returns the "short form" of the DOI that is used internally at PLOS for a variety of purposes.
   * <p/>
   * For example, "info:doi/10.1371/journal.ppat.1003156" returns "ppat.1003156"
   *
   * @param doi a PLOS DOI
   * @return the short form
   */
  public static String getShortIdentifier(String doi) {
    if (doi == null) {
      throw new IllegalArgumentException("null is not a valid PLOS DOI");
    }
    Matcher m = SHORT_IDENTIFIER_RE.matcher(doi);
    if (!m.find()) {
      throw new IllegalArgumentException("Not a valid PLOS DOI: " + doi);
    }
    return m.group();
  }
}
