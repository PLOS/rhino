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

package org.ambraproject.rhino.config;

import org.ambraproject.rhino.BaseRhinoTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;


/**
 * Meta-test to check whether the Spring/JUnit4 testing harness is configured correctly.
 */
public class SanityCheckTest extends BaseRhinoTest {

  @Autowired
  private Object sampleBean;

  /**
   * Check that beans configured in {@link TestConfiguration} are accessible in the {@code applicationContext} provided
   * by the framework's testing superclass.
   */
  @Test
  public void testApplicationContext() {
    logger.debug("testApplicationContext");
    assertNotNull(applicationContext);
    assertNotNull(applicationContext.getBean("sampleBean"));
  }

  /**
   * Check that beans configured in {@link TestConfiguration} are autowired correctly.
   */
  @Test
  public void testAutowiring() {
    logger.debug("testAutowiring");
    assertNotNull(sampleBean);
  }

}
