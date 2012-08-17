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

package org.ambraproject.admin.org.ambraproject.admin.service;

import com.google.common.primitives.Bytes;
import org.ambraproject.admin.BaseAdminTest;
import org.ambraproject.admin.RestClientException;
import org.ambraproject.admin.service.ArticleCrudService;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class ArticleCrudServiceTest extends BaseAdminTest {

  @Autowired
  private ArticleCrudService articleCrudService;

  /**
   * In addition to checking the existence of the service, this will throw an exception under certain error conditions
   * in the configuration of the Hibernate environment.
   */
  @Test
  public void testServiceAutowiring() {
    assertNotNull(articleCrudService);
  }

  private void assertArticleExistence(String doi, boolean expectedToExist) throws FileStoreException {
    boolean received404 = false;
    try {
      articleCrudService.read(doi);
    } catch (RestClientException e) {
      if (HttpStatus.NOT_FOUND.equals(e.getResponseStatus())) {
        received404 = true;
      }
    }
    assertEquals(received404, !expectedToExist,
        (expectedToExist ? "Article expected to exist but doesn't" : "Article expected not to exist but does"));
  }

  @Test(dataProvider = "sampleArticles")
  public void testCrud(String doi, File fileLocation) throws IOException, FileStoreException {
    doi += ".testCrud"; // Avoid collisions with canonical sample data

    final TestFile sampleFile = new TestFile(fileLocation);
    final byte[] sampleData = sampleFile.getData();

    assertArticleExistence(doi, false);

    TestInputStream input = sampleFile.read();
    articleCrudService.create(input, doi);
    assertArticleExistence(doi, true);
    assertTrue(input.isClosed(), "Service didn't close stream");

    Article stored = (Article) hibernateTemplate.findByCriteria(DetachedCriteria
        .forClass(Article.class).add(Restrictions.eq("doi", doi)))
        .get(0);
    assertNotNull(stored, "ArticleCrudService.create did not store an article");
    assertEquals(stored.getDoi(), doi);

    String storedDescription = stored.getDescription();
    assertTrue(StringUtils.isNotBlank(storedDescription), "Description was not set on article");
    assertTrue(storedDescription.trim().length() == storedDescription.length(),
        "Description was stored with whitespace");

    byte[] readData = articleCrudService.read(doi);
    assertEquals(readData, sampleData);

    final byte[] updated = Bytes.concat(sampleData, "\n<!-- Appended -->".getBytes());
    input = TestInputStream.of(updated);
    articleCrudService.update(input, doi);
    assertEquals(articleCrudService.read(doi), updated);
    assertArticleExistence(doi, true);
    assertTrue(input.isClosed(), "Service didn't close stream");

    articleCrudService.delete(doi);
    assertArticleExistence(doi, false);
  }

  @Test(dataProvider = "sampleArticles")
  public void testCreateCollision(String doi, File fileLocation) throws IOException, FileStoreException {
    doi += ".testCreateCollision"; // Avoid collisions with canonical sample data

    final TestFile sampleFile = new TestFile(fileLocation);

    assertArticleExistence(doi, false);

    articleCrudService.create(sampleFile.read(), doi);
    assertArticleExistence(doi, true);

    try {
      articleCrudService.create(sampleFile.read(), doi);
      fail("Expected RestClientException on redundant create");
    } catch (RestClientException e) {
      assertEquals(e.getResponseStatus(), HttpStatus.METHOD_NOT_ALLOWED);
    }
  }

}
