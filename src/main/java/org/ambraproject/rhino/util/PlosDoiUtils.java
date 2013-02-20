/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2013 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.util;

import com.google.common.base.Preconditions;

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

  private PlosDoiUtils() {
  }

  /**
   * Validates that the inpyut is a well-formed PLOS DOI.
   */
  public static boolean validate(String doi) {
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
    Preconditions.checkNotNull(doi);
    Matcher m = SHORT_IDENTIFIER_RE.matcher(doi);
    if (!m.find()) {
      throw new IllegalArgumentException("Not a valid PLOS DOI: " + doi);
    }
    return m.group();
  }
}
