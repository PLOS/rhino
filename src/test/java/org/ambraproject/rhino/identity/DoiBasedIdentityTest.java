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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 *
 */
public class DoiBasedIdentityTest {

  @DataProvider
  public Object[][] badDois() {
    Object[] bad = new Object[] {
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
      results[i] = new Object[] {bad[i]};
    }
    return results;
  }

  @Test
  public void testCreate() {
    assertEquals(DoiBasedIdentity.create("info:doi/10.1371/journal.pone.0056866").getIdentifier(),
        "10.1371/journal.pone.0056866");
    assertEquals(DoiBasedIdentity.create("10.1371/journal.pone.0056866").getIdentifier(),
        "10.1371/journal.pone.0056866");
    assertEquals(DoiBasedIdentity.create("10.1371/journal.pone.0055747.t004").getIdentifier(),
        "10.1371/journal.pone.0055747.t004");
    assertEquals(DoiBasedIdentity.create("info:doi/10.1371/volume.pgen.v05").getIdentifier(),
        "10.1371/volume.pgen.v05");
  }

  @Test(dataProvider = "badDois", expectedExceptions = {IllegalArgumentException.class})
  public void testCreate_error(String doi) {
    DoiBasedIdentity.create(doi);
  }

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

  @Test(dataProvider = "badDois", expectedExceptions = {IllegalArgumentException.class})
  public void testGetShortIdentifier_error(String doi) {
    DoiBasedIdentity.getShortIdentifier(doi);
  }
}
