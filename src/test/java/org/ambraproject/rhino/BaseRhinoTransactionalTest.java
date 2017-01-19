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

import com.google.gson.Gson;
import org.ambraproject.rhino.config.TestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;

/**
 * Base class for a test where each test method is run in its own Spring/Hibernate transaction.
 * <p/>
 * TODO: make BaseRhinoTest transactional, and remove this.  IngestionTest currently breaks when this is done.
 */
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = TestConfiguration.class)
public class BaseRhinoTransactionalTest extends AbstractTransactionalTestNGSpringContextTests {

  @Autowired
  protected HibernateTemplate hibernateTemplate;

  @Autowired
  protected Gson entityGson;

  /**
   * Clear out old persistent values.
   * <p/>
   * Really, the entire persistent environment should be reset on every test. Deleting values that we expected to be
   * created by type is a kludge.
   */
  @BeforeMethod
  public void deleteEntities() {
    RhinoTestHelper.deleteEntities(hibernateTemplate);
  }

  @DataProvider
  public Object[][] sampleArticles() {
    return RhinoTestHelper.sampleArticles();
  }

  /**
   * Adds journal entities for the test article set.
   */
  protected void addExpectedJournals() {
    RhinoTestHelper.addExpectedJournals(hibernateTemplate);
  }
}
