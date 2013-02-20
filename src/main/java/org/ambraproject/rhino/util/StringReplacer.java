/*
 * Copyright (c) 2006-2013 by Public Library of Science
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

package org.ambraproject.rhino.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A sequence of replace operations to perform on a string.
 * <p/>
 * A replacement done with this class is equivalent to a series of {@link String#replaceAll} calls, but with some
 * arguable advantages. This class may help with code cleanliness by allowing a reused set of find-and-replace values as
 * one object. Also, it caches the {@link Pattern} objects that {@link String#replaceAll} would compile each time it is
 * called, which has a potential (but as-yet unmeasured) performance benefit.
 */
public class StringReplacer {

  private static class ReplacementCase {
    private final String target;
    private final String replacement;
    private final Pattern regex;

    private ReplacementCase(String target, String replacement) {
      this.target = Preconditions.checkNotNull(target);
      this.replacement = Preconditions.checkNotNull(replacement);
      this.regex = Pattern.compile(target, Pattern.LITERAL);
    }

    private String replace(CharSequence text) {
      return regex.matcher(text).replaceAll(replacement);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ReplacementCase that = (ReplacementCase) o;

      if (!target.equals(that.target)) return false;
      if (!replacement.equals(that.replacement)) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = 1;
      result = 31 * result + target.hashCode();
      result = 31 * result + replacement.hashCode();
      return result;
    }
  }

  private final ImmutableList<ReplacementCase> replacementCases;

  private StringReplacer(List<ReplacementCase> replacementCases) {
    this.replacementCases = ImmutableList.copyOf(replacementCases);
  }

  /**
   * Apply this object's replacements. The replacements are sequential, so the string built by performing one
   * replacement will be eligible for modification by the next.
   *
   * @param text the text to search
   * @return the text with matched strings replaced
   */
  public String replace(CharSequence text) {
    String s = text.toString();
    for (ReplacementCase r : replacementCases) {
      s = r.replace(s);
    }
    return s;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private List<ReplacementCase> replacementCases;

    private Builder() {
      this.replacementCases = Lists.newArrayList();
    }

    /**
     * Register a replacement for the built object to perform. Order is significant; replacements will be done in the
     * same order as the calls to this method.
     *
     * @param target      the exact substring to search for
     * @param replacement the string to substitute when the target is found
     * @return this builder object, for chaining
     */
    public Builder add(String target, String replacement) {
      replacementCases.add(new ReplacementCase(target, replacement));
      return this;
    }

    public StringReplacer build() {
      return new StringReplacer(replacementCases);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    StringReplacer that = (StringReplacer) o;
    return replacementCases.equals(that.replacementCases);
  }

  @Override
  public int hashCode() {
    return replacementCases.hashCode();
  }

}
