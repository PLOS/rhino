/*
 * Copyright (c) 2006-2012 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.identity;

import com.google.common.base.Preconditions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An entity identifier based on a Digital Object Identifier (DOI). Instances of this class cover two cases: <ol>
 * <li>The entity is an article, in which case the {@link #getIdentifier() identifier} is the article's actual DOI that
 * would be known to public DOI resolvers.</li> <li>The entity is not an article, but has a "DOI-like" identifier. In
 * this case, the {@link #getIdentifier() identifier} is not a true DOI that a resolver would recognize. (However, such
 * asset IDs use the parent article's DOI as a prefix, so in that sense they are "DOI-based".)</li> </ol>
 */
public class DoiBasedIdentity {

  protected static final String DOI_SCHEME_VALUE = "info:doi/";
  protected static final String XML_EXTENSION = "xml";

  private final String identifier; // non-null, non-empty, doesn't contain ':'

  protected DoiBasedIdentity(String identifier) {
    super();
    this.identifier = Preconditions.checkNotNull(identifier);

    Preconditions.checkArgument(identifier.indexOf(':') < 0, "DOI must not have scheme prefix (\"info:doi/\")");
    Preconditions.checkArgument(!identifier.isEmpty(), "DOI is an empty string");
  }

  public static DoiBasedIdentity create(String identifier) {
    return new DoiBasedIdentity(identifier);
  }

  /**
   * Remove the leading {@code "info:doi/"} from a DOI. If the DOI does not start with {@code "info:doi/"}, return it
   * unchanged.
   *
   * @param doi a non-null DOI
   * @return a DOI that does not start with {@code "info:doi/"}
   */
  public static String removeScheme(String doi) {
    return doi.startsWith(DOI_SCHEME_VALUE) ? doi.substring(DOI_SCHEME_VALUE.length()) : doi;
  }

  private static final Pattern SHORT_IDENTIFIER_RE = Pattern.compile("p[a-z]{3}\\.\\d{7}");

  /**
   * Returns the "short form" of the DOI that is used internally at PLOS
   * for a variety of purposes.
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

  /**
   * Return the DOI or DOI-like identifier for the article or asset that this object identifies. The return value will
   * not be prefixed with {@code "info:doi/"} and, under Ambra's current data model, must not be stored in a DOI column
   * of any database table.
   * <p/>
   * The value {@code this.getIdentifier()} represents an actual DOI (that a public DOI resolver would know about) if
   * and only if {@code this.isAsset()} returns {@code false}.
   *
   * @return the DOI or DOI-like identifier
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * Return a database key value to represent the article or asset that this object identifies. Under Ambra's current
   * data model, this is the string returned by {@link #getIdentifier} prefixed with {@code "info:doi/"}.
   *
   * @return the key value
   */
  public String getKey() {
    return DOI_SCHEME_VALUE + identifier;
  }

  @Override
  public String toString() {
    return getIdentifier();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DoiBasedIdentity that = (DoiBasedIdentity) o;

    if (identifier != null ? !identifier.equals(that.identifier) : that.identifier != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return identifier != null ? identifier.hashCode() : 0;
  }

}
