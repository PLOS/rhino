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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link PlosDoiUtils}.
 */
public class PlosDoiUtilsTest {

  @DataProvider
  public Object[][] badDois() {
    Object[] bad = new Object[]{
        null,
        "",
        "info:",
        "info:doi/10.1371/",
        "info:doi/10.1371/journal.pone.",
        "info:doi/10.1371/journal.pone.1234",
        "info:doi/10.1371/journal.pone.005305",
        "info:doi/10.1371/image.pntd.v07.i01",
        "10.1371",
        "10.1371/",
        "10.1371/journal.",
        "10.1371/journal.crap.0056866",
    };
    Object[][] results = new Object[bad.length][];
    for (int i = 0; i < bad.length; i++) {
      results[i] = new Object[]{bad[i]};
    }
    return results;
  }

  @Test
  public void testValidate_success() {
    assertTrue(PlosDoiUtils.validate("info:doi/10.1371/journal.pone.0056866"));
    assertTrue(PlosDoiUtils.validate("10.1371/journal.pone.0056866"));
    assertTrue(PlosDoiUtils.validate("10.1371/journal.pone.0055747.t004"));
    assertTrue(PlosDoiUtils.validate("info:doi/10.1371/volume.pgen.v05"));
  }

  @Test(dataProvider = "badDois")
  public void testValidate_error(String doi) {
    assertFalse(PlosDoiUtils.validate(doi));
  }

  @Test
  public void testGetShortIdentifier() {
    assertEquals(PlosDoiUtils.getShortIdentifier("info:doi/10.1371/journal.pone.0053052"),
        "pone.0053052");
    assertEquals(PlosDoiUtils.getShortIdentifier("10.1371/journal.pgen.1003285"),
        "pgen.1003285");
    assertEquals(PlosDoiUtils.getShortIdentifier("pntd.0002035"), "pntd.0002035");
    assertEquals(PlosDoiUtils.getShortIdentifier("journal.ppat.1003160"), "ppat.1003160");
    assertEquals(PlosDoiUtils.getShortIdentifier("info:doi/10.1371/journal.pcbi.1002905.g002"),
        "pcbi.1002905");
  }

  @Test(dataProvider = "badDois", expectedExceptions = {IllegalArgumentException.class})
  public void testGetShortIdentifier_error(String doi) {
    PlosDoiUtils.getShortIdentifier(doi);
  }
}
