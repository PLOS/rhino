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

package org.ambraproject.rhino.identity;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.URI;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.stream.Stream;

public class DoiTest {

  private static class StylePair {
    private final Doi.UriStyle from;
    private final Doi.UriStyle to;

    private StylePair(Doi.UriStyle from, Doi.UriStyle to) {
      this.from = from;
      this.to = to;
    }
  }

  private static Stream<StylePair> getStyleMatrix() {
    return EnumSet.allOf(Doi.UriStyle.class).stream().flatMap(from ->
        EnumSet.allOf(Doi.UriStyle.class).stream().map(to -> new StylePair(from, to)));
  }

  @Test
  public void checkForPrefixCollisions() {
    getStyleMatrix().forEach(stylePair -> {
      if (stylePair.from != stylePair.to) {
        Assert.assertFalse(stylePair.from.getPrefix().startsWith(stylePair.to.getPrefix()));
      }
    });
  }

  @DataProvider
  public Iterator<Object[]> doiCases() {
    String[] doiNames = new String[]{"10.1371/foo"};
    return Stream.of(doiNames)
        .flatMap(doiName -> getStyleMatrix()
            .map(stylePair -> new Object[]{doiName, stylePair.from, stylePair.to}))
        .iterator();
  }

  @Test(dataProvider = "doiCases")
  public void testDoi(String doiName, Doi.UriStyle from, Doi.UriStyle to) {
    Doi fromRawName = Doi.create(doiName);
    URI asUri = fromRawName.asUri(from);
    Assert.assertEquals(asUri.toString(), from.getPrefix() + doiName);

    Doi fromUri = Doi.create(asUri.toString());
    Assert.assertEquals(fromUri, fromRawName);

    Doi converted = Doi.create(fromUri.asUri(to).toString());
    Assert.assertEquals(converted, fromUri);
    Assert.assertEquals(converted, fromRawName);
  }

  @Test
  public void testCaseInsensitivity() {
    Doi lowercase = Doi.create("10.1371/foo/bar");
    Doi uppercase = Doi.create("10.1371/FOO/bar");
    Assert.assertTrue(lowercase.equals(uppercase));
    Assert.assertTrue(lowercase.hashCode() == uppercase.hashCode());
  }

}
