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

package org.ambraproject.rhino;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.InputStream;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.w3c.dom.Document;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.ambraproject.rhino.config.TestConfiguration;
import org.ambraproject.rhino.service.impl.AmbraService;
import org.junit.Before;

@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = TestConfiguration.class)

// TODO: try to get the tests to work transactionally--I was having much pain with this.
public abstract class BaseRhinoTest extends /* AbstractTransactionalJUnit4SpringContextTests */ AbstractJUnit4SpringContextTests {

  @Autowired
  protected HibernateTemplate hibernateTemplate;

  @Autowired
  protected Gson entityGson;

  /**
   * Adds journal entities for the test article set.
   */
  protected void addExpectedJournals() {
    RhinoTestHelper.addExpectedJournals(hibernateTemplate);
  }

  private static final File DATA_PATH = new File("src/test/resources/articles/");

  public static Document parseTestFile(String filename) throws IOException {
    File testFile = new File(DATA_PATH, filename);
    try (InputStream testData = new BufferedInputStream(new FileInputStream(testFile))) {
      return AmbraService.parseXml(testData);
    }
  }
}
