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

import org.ambraproject.admin.BaseAdminTest;
import org.ambraproject.admin.RestClientException;
import org.ambraproject.admin.service.ArticleCrudService;
import org.ambraproject.filestore.FileStoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

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
    assertEquals(received404, !expectedToExist);
  }

  @Test
  public void testCrud() throws IOException, FileStoreException {
    final String data = "Imagine this is some XML data.";
    final String doi = "test/crud";
    assertArticleExistence(doi, false);

    TestInputStream input = new TestInputStream(data);
    articleCrudService.create(input, doi);
    assertArticleExistence(doi, true);
    assertTrue(input.isClosed());

    byte[] readData = articleCrudService.read(doi);
    assertEquals(readData, data.getBytes());

    final String updated = data + "\nThis is appended";
    input = new TestInputStream(updated);
    articleCrudService.update(input, doi);
    assertEquals(articleCrudService.read(doi), updated.getBytes());
    assertArticleExistence(doi, true);
    assertTrue(input.isClosed());

    articleCrudService.delete(doi);
    assertArticleExistence(doi, false);
  }

  @Test
  public void testCreateCollision() throws IOException, FileStoreException {
    final String data = "Imagine this is some XML data.";
    final String doi = "test/createCollision";
    assertArticleExistence(doi, false);

    articleCrudService.create(new TestInputStream(data), doi);
    assertArticleExistence(doi, true);

    try {
      articleCrudService.create(new TestInputStream(data), doi);
      fail("Expected RestClientException on redundant create");
    } catch (RestClientException e) {
      assertEquals(e.getResponseStatus(), HttpStatus.METHOD_NOT_ALLOWED);
    }
  }

}
