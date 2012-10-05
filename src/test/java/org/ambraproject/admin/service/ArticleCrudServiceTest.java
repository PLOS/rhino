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

package org.ambraproject.admin.service;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.common.primitives.Bytes;
import org.ambraproject.admin.BaseAdminTest;
import org.ambraproject.admin.RestClientException;
import org.ambraproject.admin.controller.DoiBasedIdentity;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.models.ArticleAuthor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ArticleCrudServiceTest extends BaseAdminTest {

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private AssetCrudService assetCrudService;

  /**
   * In addition to checking the existence of the service, this will throw an exception under certain error conditions
   * in the configuration of the Hibernate environment.
   */
  @Test
  public void testServiceAutowiring() {
    assertNotNull(articleCrudService);
  }


  private static DoiBasedIdentity identifyAsset(String assetDoi, String extension) {
    return DoiBasedIdentity.parse(assetDoi + '.' + extension, true);
  }

  private void assertArticleExistence(DoiBasedIdentity id, boolean expectedToExist) throws FileStoreException {
    boolean received404 = false;
    try {
      articleCrudService.read(id);
    } catch (RestClientException e) {
      if (HttpStatus.NOT_FOUND.equals(e.getResponseStatus())) {
        received404 = true;
      } else {
        throw e;
      }
    }
    assertEquals(received404, !expectedToExist,
        (expectedToExist ? "Article expected to exist but doesn't" : "Article expected not to exist but does"));
  }

  private void assertGoodText(String text) {
    assertNotNull(text, "Text field was not set");
    assertFalse(text.isEmpty(), "Text field was set to an empty string");
    assertFalse(Character.isWhitespace(text.charAt(0)), "Text field was set with leading whitespace");
    assertFalse(Character.isWhitespace(text.charAt(text.length() - 1)),
        "Text field was set with trailing whitespace");
  }

  @Test(dataProvider = "sampleArticles")
  public void testCrud(String doi, File fileLocation) throws IOException, FileStoreException {
    doi += ".testCrud"; // Avoid collisions with canonical sample data

    final TestFile sampleFile = new TestFile(fileLocation);
    final byte[] sampleData = sampleFile.getData();
    final DoiBasedIdentity articleId = DoiBasedIdentity.forArticle(doi);
    final String key = articleId.getKey();

    assertArticleExistence(articleId, false);

    TestInputStream input = sampleFile.read();
    articleCrudService.write(input, Optional.of(articleId), null);
    assertArticleExistence(articleId, true);
    assertTrue(input.isClosed(), "Service didn't close stream");

    Article stored = (Article) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Article.class)
            .add(Restrictions.eq("doi", key))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    assertNotNull(stored, "ArticleCrudService.create did not store an article");
    assertEquals(stored.getDoi(), key);
    assertEquals(stored.getLanguage(), "en");
    assertGoodText(stored.getDescription());
    assertGoodText(stored.getRights());

    List<ArticleAuthor> storedAuthors = stored.getAuthors();
    assertNotNull(storedAuthors, "Article's authors field was not set");
    assertFalse(storedAuthors.isEmpty(), "No authors were associated with the article");

    // Check that no author names were stored redundantly
    Set<String> authorNames = Sets.newHashSetWithExpectedSize(storedAuthors.size());
    for (ArticleAuthor author : storedAuthors) {
      String fullName = author.getFullName();
      assertFalse(StringUtils.isBlank(fullName), "Name not set for author");
      assertTrue(authorNames.add(fullName), "Redundant author name");
    }

    byte[] readData = IOUtils.toByteArray(articleCrudService.read(articleId));
    assertEquals(readData, sampleData);

    final byte[] updated = Bytes.concat(sampleData, "\n<!-- Appended -->".getBytes());
    input = TestInputStream.of(updated);
    articleCrudService.write(input, Optional.of(articleId), null);
    byte[] updatedData = IOUtils.toByteArray(articleCrudService.read(articleId));
    assertEquals(updatedData, updated);
    assertArticleExistence(articleId, true);
    assertTrue(input.isClosed(), "Service didn't close stream");

    articleCrudService.delete(articleId);
    assertArticleExistence(articleId, false);
  }

  @Test(dataProvider = "sampleAssets")
  public void testCreateAsset(String articleDoi, File articleFile, String assetDoi, File assetFile)
      throws IOException, FileStoreException {
    articleDoi += ".testCreateAsset"; // Avoid collisions with canonical sample data

    String assetFilePath = assetFile.getPath();
    String extension = assetFilePath.substring(assetFilePath.lastIndexOf('.') + 1);
    final DoiBasedIdentity assetId = identifyAsset(assetDoi, extension);
    final DoiBasedIdentity articleId = DoiBasedIdentity.forArticle(articleDoi);

    articleCrudService.write(new TestFile(articleFile).read(), Optional.of(articleId), null);

    TestInputStream assetFileStream = new TestFile(assetFile).read();
    assetCrudService.upload(assetFileStream, assetId, Optional.of(articleId));

    ArticleAsset stored = (ArticleAsset) DataAccessUtils.uniqueResult(
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(ArticleAsset.class)
            .add(Restrictions.eq("doi", assetId.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    assertNotNull(stored);
  }

}
