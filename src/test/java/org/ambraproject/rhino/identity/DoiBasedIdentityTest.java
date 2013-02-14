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

package org.ambraproject.rhino.identity;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 *
 */
public class DoiBasedIdentityTest {

  @Test
  public void testGetShortIdentifier() {
    assertEquals(DoiBasedIdentity.getShortIdentifier("info:doi/10.1371/journal.pone.0053052"),
        "pone.0053052");
    assertEquals(DoiBasedIdentity.getShortIdentifier("10.1371/journal.pgen.1003285"),
        "pgen.1003285");
    assertEquals(DoiBasedIdentity.getShortIdentifier("pntd.0002035"), "pntd.0002035");
    assertEquals(DoiBasedIdentity.getShortIdentifier("journal.ppat.1003160"), "ppat.1003160");
    assertEquals(DoiBasedIdentity.getShortIdentifier("info:doi/10.1371/journal.pcbi.1002905.g002"),
        "pcbi.1002905");
  }

  @Test
  public void testGetShortIdentifier_error() {

    // Not using expectedExceptions on the @Test annotation here since we want
    // to test a bunch of cases.
    getShortIdentifierWithExpectedException("");
    getShortIdentifierWithExpectedException("info:");
    getShortIdentifierWithExpectedException("info:doi/10.1371/");
    getShortIdentifierWithExpectedException("info:doi/10.1371/journal.pone.");
    getShortIdentifierWithExpectedException("info:doi/10.1371/journal.pone.1234");
    getShortIdentifierWithExpectedException("info:doi/10.1371/journal.pone.005305");
    getShortIdentifierWithExpectedException("info:doi/10.1371/image.pntd.v07.i01");
  }

  private void getShortIdentifierWithExpectedException(String s) {
    try {
      DoiBasedIdentity.getShortIdentifier(s);
    } catch (IllegalArgumentException iae) {
      return;
    }
    fail("getShortIdentifier did not throw IllegalArgumentException for " + s);
  }
}
