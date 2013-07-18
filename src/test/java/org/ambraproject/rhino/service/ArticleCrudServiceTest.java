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

package org.ambraproject.rhino.service;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.primitives.Bytes;
import com.google.gson.Gson;
import org.ambraproject.filestore.FileStoreException;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.models.ArticleAuthor;
import org.ambraproject.models.Category;
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.DoiBasedCrudService.WriteMode;
import org.ambraproject.rhino.service.impl.ArticleCrudServiceImpl;
import org.ambraproject.rhino.test.DummyResponseReceiver;
import org.ambraproject.rhino.view.article.ArticleCriteria;
import org.ambraproject.rhino.view.article.DoiList;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ArticleCrudServiceTest extends BaseRhinoTest {

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

  /**
   * Create journals with all eIssn values mentioned in test cases' XML files.
   */
  @BeforeMethod
  public void addJournal() {
    addExpectedJournals();
  }

  private void assertArticleExistence(ArticleIdentity id, boolean expectedToExist) throws FileStoreException {
    boolean received404 = false;
    try {
      articleCrudService.readXml(id);
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
    final ArticleIdentity articleId = ArticleIdentity.create(doi);
    final String key = articleId.getKey();

    final TestFile sampleFile = new TestFile(fileLocation);
    final byte[] sampleData = IOUtils.toByteArray(alterStream(sampleFile.read(), doi, doi));

    assertArticleExistence(articleId, false);

    TestInputStream input = TestInputStream.of(sampleData);
    Article article = articleCrudService.write(input, Optional.of(articleId), WriteMode.CREATE_ONLY);
    assertArticleExistence(articleId, true);
    assertTrue(input.isClosed(), "Service didn't close stream");

    Article stored = (Article) DataAccessUtils.uniqueResult((List<?>)
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

    Set<Category> expectedCategories = new HashSet<Category>();
    Category cat1 = new Category();
    cat1.setPath("/TopLevel1/term1");
    expectedCategories.add(cat1);
    Category cat2 = new Category();
    cat2.setPath("/TopLevel2/term2");
    expectedCategories.add(cat2);
    assertEquals(stored.getCategories(), expectedCategories);

    byte[] readData = IOUtils.toByteArray(articleCrudService.readXml(articleId));
    assertEquals(readData, sampleData);

    final byte[] updated = Bytes.concat(sampleData, "\n<!-- Appended -->".getBytes());
    input = TestInputStream.of(updated);
    article = articleCrudService.write(input, Optional.of(articleId), WriteMode.UPDATE_ONLY);
    byte[] updatedData = IOUtils.toByteArray(articleCrudService.readXml(articleId));
    assertEquals(updatedData, updated);
    assertArticleExistence(articleId, true);
    assertTrue(input.isClosed(), "Service didn't close stream");

    articleCrudService.delete(articleId);
    assertArticleExistence(articleId, false);
  }

  @Test(dataProvider = "sampleAssets")
  public void testCreateAsset(String articleDoi, File articleFile, String assetDoi, File assetFile)
      throws IOException, FileStoreException {
    String testAssetDoi = assetDoi.replace(articleDoi, articleDoi);

    String assetFilePath = assetFile.getPath();
    String extension = assetFilePath.substring(assetFilePath.lastIndexOf('.') + 1);
    final AssetFileIdentity assetId = AssetFileIdentity.create(testAssetDoi, extension);
    final ArticleIdentity articleId = ArticleIdentity.create(articleDoi);
    TestInputStream input = new TestFile(articleFile).read();
    input = alterStream(input, articleDoi, articleDoi);
    Article article = articleCrudService.write(input, Optional.of(articleId), WriteMode.CREATE_ONLY);

    TestInputStream assetFileStream = new TestFile(assetFile).read();
    assetCrudService.upload(assetFileStream, assetId);

    ArticleAsset stored = (ArticleAsset) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(ArticleAsset.class)
            .add(Restrictions.eq("doi", assetId.getKey()))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
        ));
    assertNotNull(stored.getContextElement());
    assertNotNull(stored.getContentType());
    assertFalse(Strings.isNullOrEmpty(stored.getExtension()));
  }

  @Test
  public void testShouldSaveAssetFile() {
    assertTrue(ArticleCrudServiceImpl.shouldSaveAssetFile("pone.0058631.g001.tif"));
    assertTrue(ArticleCrudServiceImpl.shouldSaveAssetFile("ppat.1003193.g002.PNG_M"));
    assertTrue(ArticleCrudServiceImpl.shouldSaveAssetFile("pcbi.1002867.pdf"));
    assertTrue(ArticleCrudServiceImpl.shouldSaveAssetFile("pone.0055746.s005.doc"));

    assertFalse(ArticleCrudServiceImpl.shouldSaveAssetFile("manifest.dtd"));
    assertFalse(ArticleCrudServiceImpl.shouldSaveAssetFile("MANIFEST.xml"));
    assertFalse(ArticleCrudServiceImpl.shouldSaveAssetFile("pone.0058631.xml"));
    assertFalse(ArticleCrudServiceImpl.shouldSaveAssetFile("ppat.1003188.xml.meta"));
    assertFalse(ArticleCrudServiceImpl.shouldSaveAssetFile("pone.0058631.xml.orig"));
  }

  @Test
  public void testListDois() throws IOException {
    Article a1 = new Article();
    a1.setDoi("info:doi/10.0/test1");
    a1.seteIssn("1932-6203");
    hibernateTemplate.save(a1);

    Article a2 = new Article();
    a2.setDoi("info:doi/10.0/test2");
    a2.seteIssn(a1.geteIssn());
    hibernateTemplate.save(a2);

    DummyResponseReceiver response = new DummyResponseReceiver();
    articleCrudService.listDois(response, MetadataFormat.JSON, ArticleCriteria.create(null, null));
    DoiList doiList = entityGson.fromJson(response.read(), DoiList.class);
    assertEquals(ImmutableSet.copyOf(doiList.getDois()), ImmutableSet.of(a1.getDoi(), a2.getDoi()));
  }

  @Test
  public void testArticleType() throws Exception {
    String doiStub = SAMPLE_ARTICLES.get(0);
    ArticleIdentity articleId = ArticleIdentity.create(prefixed(doiStub));
    TestFile sampleFile = new TestFile(new File("src/test/resources/articles/" + doiStub + ".xml"));
    String doi = articleId.getIdentifier();
    byte[] sampleData = IOUtils.toByteArray(alterStream(sampleFile.read(), doi, doi));
    TestInputStream input = TestInputStream.of(sampleData);
    Article article = articleCrudService.write(input, Optional.of(articleId),
        DoiBasedCrudService.WriteMode.CREATE_ONLY);

    DummyResponseReceiver drr = new DummyResponseReceiver();
    articleCrudService.readMetadata(drr, articleId, MetadataFormat.JSON);
    String json = drr.read();
    assertTrue(json.length() > 0);
    Gson gson = new Gson();
    Map<?, ?> articleMap = gson.fromJson(json, Map.class);
    assertEquals(articleMap.get("doi"), articleId.getKey());
    assertEquals(articleMap.get("title"), article.getTitle());
    assertEquals(articleMap.get("articleType"), "Research Article");
  }
}
