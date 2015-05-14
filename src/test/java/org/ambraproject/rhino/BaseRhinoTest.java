/*
 * Copyright (c) 2006-2012 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino;

import com.google.gson.Gson;
import org.ambraproject.models.Article;
import org.ambraproject.rhino.config.TestConfiguration;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.util.Archive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;

import java.io.IOException;

@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = TestConfiguration.class)

// TODO: try to get the tests to work transactionally--I was having much pain with this.
public abstract class BaseRhinoTest extends /* AbstractTransactionalTestNGSpringContextTests */ AbstractTestNGSpringContextTests {

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

  public static Article writeToLegacy(ArticleCrudService articleCrudService, Archive ingestible) throws IOException {
    ArticleCrudService.IngestionResult result = articleCrudService.writeArchive(ingestible);
    return articleCrudService.writeToLegacy(result.getCollection());
  }

}
