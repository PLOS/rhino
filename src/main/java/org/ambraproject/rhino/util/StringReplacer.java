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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A sequence of replace operations to perform on a string. It stores pre-compiled {@link Pattern} objects and, for code
 * cleanliness, pairs them with {@code replaceAll} arguments to apply in a batch. It may be preferable to making
 * repeated calls to {@link String#replaceAll} for performance reasons, because {@link String#replaceAll} must compile a
 * new {@link Pattern} each time it is called.
 * <p/>
 * This class and its cached {@link Pattern} objects are immutable and thread-safe.
 * <p/>
 * TODO: Delete this and use org.ambraproject.util.StringReplacer instead as soon as it's imported from Ambra Base.
 */
public class StringReplacer {

  private static class ReplacementCase {
    private final Pattern regex;
    private final String replacement;

    private ReplacementCase(Pattern regex, String replacement) {
      this.regex = Preconditions.checkNotNull(regex);
      this.replacement = Preconditions.checkNotNull(replacement);
    }

    private String replace(CharSequence text) {
      return regex.matcher(text).replaceAll(replacement);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ReplacementCase that = (ReplacementCase) o;
      if (!replacement.equals(that.replacement)) return false;
      if (!regex.toString().equals(that.regex.toString())) return false;
      if (regex.flags() != that.regex.flags()) return false;
      return true;
    }

    @Override
    public int hashCode() {
      int result = 1;
      result = 31 * result + replacement.hashCode();
      result = 31 * result + regex.toString().hashCode();
      result = 31 * result + regex.flags();
      return result;
    }

    @Override
    public String toString() {
      return String.format("(Pattern.compile(\"%s\", %d), \"%s\")",
          StringEscapeUtils.escapeJava(regex.toString()), regex.flags(),
          StringEscapeUtils.escapeJava(replacement));
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

  /**
   * Create a builder object. The order in which the builder's methods are called is significant: calls to {@link
   * StringReplacer#replace} on the resulting object will perform the replacements in the same order that they are
   * registered on the builder.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private List<ReplacementCase> replacementCases;

    private Builder() {
      this.replacementCases = Lists.newArrayList();
    }

    /**
     * Set up the built object to a replace an exact string.
     *
     * @param target      the exact substring to search for
     * @param replacement the string to substitute when the target is found
     * @return this builder object, for chaining
     */
    public Builder replaceExact(String target, String replacement) {
      return replaceRegex(Pattern.compile(target, Pattern.LITERAL), replacement);
    }

    /**
     * Set up the built object to a replace a regular expression. This is a convenience method for regular expressions
     * with no flags. To use a more complex regular expression, {@link Pattern#compile} it and then pass it to {@link
     * #replaceRegex(java.util.regex.Pattern, String)}.
     *
     * @param target      the regular expression to search for
     * @param replacement the string to substitute when the target is matched
     * @return this builder object, for chaining
     */
    public Builder replaceRegex(String target, String replacement) {
      return replaceRegex(Pattern.compile(target), replacement);
    }

    /**
     * Set up the built object to a replace a regular expression.
     *
     * @param target      the compiled regular expression to search for
     * @param replacement the string to substitute when the target is matched
     * @return this builder object, for chaining
     */
    public Builder replaceRegex(Pattern target, String replacement) {
      replacementCases.add(new ReplacementCase(target, replacement));
      return this;
    }

    /**
     * Set up the built object to a delete an exact string.
     *
     * @param target the exact substring to search for
     * @return this builder object, for chaining
     */
    public Builder deleteExact(String target) {
      return replaceExact(target, "");
    }

    /**
     * Set up the built object to a delete a regular expression that has no flags.
     *
     * @param target the regular expression to search for
     * @return this builder object, for chaining
     */
    public Builder deleteRegex(String target) {
      return replaceRegex(target, "");
    }

    /**
     * Set up the built object to a delete a regular expression.
     *
     * @param target the compiled regular expression to search for
     * @return this builder object, for chaining
     */
    public Builder deleteRegex(Pattern target) {
      return replaceRegex(target, "");
    }

    /**
     * Construct an immutable {@code StringReplacer} from this builder. The builder's state will be unaffected, and it
     * may be added to and used to build more objects.
     *
     * @return the new {@code StringReplacer}
     */
    public StringReplacer build() {
      return new StringReplacer(replacementCases);
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + '{' + replacementCases + '}';
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
