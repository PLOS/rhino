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

import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class DoiTest {

  /**
   * Return the cartesian product of URI styles as a set of pairs.
   */
  private static List<Pair<Doi.UriStyle, Doi.UriStyle>> getStyleMatrix() {
    List<Pair<Doi.UriStyle, Doi.UriStyle>> retval = new ArrayList<>();
    for (Doi.UriStyle from: EnumSet.allOf(Doi.UriStyle.class)) {
      for (Doi.UriStyle to: EnumSet.allOf(Doi.UriStyle.class)) {
        retval.add(Pair.of(from, to));
      }
    }
    return retval;
  }

  @Test
  public void checkForPrefixCollisions() {
    getStyleMatrix().forEach(stylePair -> {
        if (stylePair.getKey() != stylePair.getValue()) {
          Assert.assertFalse(stylePair.getKey().getPrefix().startsWith(stylePair.getValue().getPrefix()));
      }
    });
  }

  @DataProvider
  public static List<List<Object>> doiCases() {
    List<List<Object>> retval = new ArrayList<>();
    
    for(String doi: ImmutableList.of("10.1371/foo")) {
      for (Pair<Doi.UriStyle, Doi.UriStyle> entry: getStyleMatrix()) {
        retval.add(ImmutableList.of(doi, entry.getKey(), entry.getValue()));
      }
    }
    return retval;
  }

  @Test
  @UseDataProvider("doiCases")
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
