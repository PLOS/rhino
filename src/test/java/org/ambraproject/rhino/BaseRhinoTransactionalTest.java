/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2014 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

  @DataProvider
  public Object[][] sampleAssets() {
    return RhinoTestHelper.sampleAssets();
  }

  /**
   * Adds journal entities for the test article set.
   */
  protected void addExpectedJournals() {
    RhinoTestHelper.addExpectedJournals(hibernateTemplate);
  }
}
