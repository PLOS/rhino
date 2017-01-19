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

import org.ambraproject.rhino.identity.Doi;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.stream.Stream;

public class DoiEscapingTest {

  // Unoptimized reference implementation. Should always return the same value as DoiEscaping.resolve
  // (excluding cases that would throw EscapedDoiException)
  private static Doi easyResolve(String escapedDoi) {
    return Doi.create(escapedDoi.replace("++", "/").replace("+-", "+"));
  }

  // All printable ASCII characters other than the ones that need escaping
  private static final String NON_ESCAPABLE = "\t\n\u000b\f\r !\"#$%&'()*,-.0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";

  @DataProvider
  public Iterator<Object[]> doiEscapingCases() {
    String[][] withoutUriPrefix = {
        {"", ""},
        {NON_ESCAPABLE, NON_ESCAPABLE},
        {"/", "++"}, {"+", "+-"}, {"++", "+-+-"}, {"+-", "+--"},
        {"//", "++++"}, {"++", "+-+-"}, {"/+", "+++-"}, {"+/", "+-++"},
        {"\u2603", "\u2603"}, {"\u2603/\u2603+\u2603", "\u2603++\u2603+-\u2603"},
        {"10.1371/journal.pone.0000000", "10.1371++journal.pone.0000000"},
        {"10.1371/annotation/877a3639-4318-4ff2-9578-28e3de88b176", "10.1371++annotation++877a3639-4318-4ff2-9578-28e3de88b176"},
        {"10.1371/journal+pone/+0000000", "10.1371++journal+-pone+++-0000000"},
        {"10.1371++journal+-pone+++-0000000", "10.1371+-+-journal+--pone+-+-+--0000000"},
        {"10.1093/comjnl/bxv087", "10.1093++comjnl++bxv087"},
        {"10.1093/comjnl+bxv087", "10.1093++comjnl+-bxv087"},
    };
    String[][] withUriPrefix = {
        {"10.1371/journal.pone.0000000", "info:doi++10.1371++journal.pone.0000000"},
        {"10.1371/journal.pone.0000000", "doi:10.1371++journal.pone.0000000"},
        {"10.1371/annotation/877a3639-4318-4ff2-9578-28e3de88b176", "info:doi++10.1371++annotation++877a3639-4318-4ff2-9578-28e3de88b176"},
        {"10.1371/annotation/877a3639-4318-4ff2-9578-28e3de88b176", "doi:10.1371++annotation++877a3639-4318-4ff2-9578-28e3de88b176"},
    };

    return Stream.concat(
        Stream.of(withoutUriPrefix).map(array -> new Object[]{array[0], array[1], false}),
        Stream.of(withUriPrefix).map(array -> new Object[]{array[0], array[1], true})
    ).iterator();
  }

  @Test(dataProvider = "doiEscapingCases")
  public void testDoiUnescaping(String expectedUnescape, String escapedDoi, boolean hasUriPrefix) {
    Doi actualUnescape = DoiEscaping.unescape(escapedDoi);
    Assert.assertEquals(actualUnescape, easyResolve(escapedDoi));
    Assert.assertEquals(actualUnescape.getName(), expectedUnescape);

    if (!hasUriPrefix) {
      String actualEscape = DoiEscaping.escape(expectedUnescape);
      Assert.assertEquals(actualEscape, escapedDoi);
    }
  }


  @DataProvider
  public Iterator<Object[]> invalidEscapedDois() {
    String[] invalidEscapedDois = new String[]{
        "/", "+", "+++", "+a", "+/", "+++a", "a+a",
        "10.1371/journal.pone.0000000", "10.1371+journal.pone.0000000", "10.1371++journal.pone.0000000+"
    };
    return Stream.of(invalidEscapedDois).map(s -> new Object[]{s}).iterator();
  }

  @Test(dataProvider = "invalidEscapedDois", expectedExceptions = {DoiEscaping.EscapedDoiException.class})
  public void testInvalidEscapedDoi(String escaped) {
    DoiEscaping.unescape(escaped);
  }

}
