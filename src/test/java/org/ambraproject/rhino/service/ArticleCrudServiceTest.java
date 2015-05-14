/*
 * Copyright (c) 2006-2014 by Public Library of Science
 *
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ambraproject.rhino.service;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.primitives.Bytes;
import com.google.gson.Gson;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.models.ArticleAuthor;
import org.ambraproject.models.Category;
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.BaseRhinoTransactionalTest;
import org.ambraproject.rhino.IngestibleUtil;
import org.ambraproject.rhino.RhinoTestHelper;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.taxonomy.DummyTaxonomyClassificationService;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.rhino.util.response.Transceiver;
import org.ambraproject.rhino.view.article.ArticleCriteria;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.plos.crepo.service.ContentRepoService;
import org.plos.crepo.service.InMemoryContentRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class ArticleCrudServiceTest extends BaseRhinoTransactionalTest {

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private AssetCrudService assetCrudService;
  @Autowired
  private ContentRepoService contentRepoService;

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

  /**
   * Empty all data from the mock ContentRepoService.
   */
  @BeforeMethod
  public void clearMockRepo() {
    ((InMemoryContentRepoService) contentRepoService).clear();
  }

  private void assertArticleExistence(ArticleIdentity id, boolean expectedToExist) {
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

  private static Archive createMockIngestible(ArticleIdentity articleId, InputStream xmlData) throws IOException {
    String archiveName = articleId.getLastToken() + ".zip";
    InputStream mockIngestible = IngestibleUtil.buildMockIngestible(xmlData);
    return Archive.readZipFileIntoMemory(archiveName, mockIngestible);
  }

  private Map<ArticleIdentity, Article> createTestArticle() throws IOException {
    String doiStub = RhinoTestHelper.SAMPLE_ARTICLES.get(0);
    ArticleIdentity articleId = ArticleIdentity.create(RhinoTestHelper.prefixed(doiStub));
    RhinoTestHelper.TestFile sampleFile = new RhinoTestHelper.TestFile(new File(
        "src/test/resources/articles/" + doiStub + ".xml"));
    String doi = articleId.getIdentifier();
    byte[] sampleData = IOUtils.toByteArray(RhinoTestHelper.alterStream(sampleFile.read(), doi, doi));
    RhinoTestHelper.TestInputStream input = RhinoTestHelper.TestInputStream.of(sampleData);
    Archive archive = createMockIngestible(articleId, input);
    Article article = BaseRhinoTest.writeToLegacy(articleCrudService, archive);

    HashMap<ArticleIdentity, Article> articleHashMap = new HashMap<>();
    articleHashMap.put(articleId, article);
    return articleHashMap;
  }

  @Test(dataProvider = "sampleArticles")
  public void testCrud(String doi, File fileLocation) throws IOException {
    final ArticleIdentity articleId = ArticleIdentity.create(doi);
    final String key = articleId.getKey();

    final RhinoTestHelper.TestFile sampleFile = new RhinoTestHelper.TestFile(fileLocation);
    final byte[] sampleData = IOUtils.toByteArray(RhinoTestHelper.alterStream(sampleFile.read(), doi, doi));

    assertArticleExistence(articleId, false);

    RhinoTestHelper.TestInputStream input = RhinoTestHelper.TestInputStream.of(sampleData);
    Article article = BaseRhinoTest.writeToLegacy(articleCrudService, createMockIngestible(articleId, input));
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

    Set<Category> expectedCategories = new HashSet<>();
    for (String categoryPath : DummyTaxonomyClassificationService.DUMMY_DATA.keySet()) {
      Category category = new Category();
      category.setPath(categoryPath);
      expectedCategories.add(category);
    }
    Set<Category> actualCategories = stored.getCategories().keySet();

    // org.hibernate.collection.AbstractPersistentCollection.SetProxy does not respect the Set.equals contract,
    // so copy actualCategories out to a well-behaved Set before comparing.
    actualCategories = new HashSet<>(actualCategories);
    assertEquals(actualCategories, expectedCategories);

    byte[] readData = IOUtils.toByteArray(articleCrudService.readXml(articleId));
    assertEquals(readData, sampleData);

    final byte[] updated = Bytes.concat(sampleData, "\n<!-- Appended -->".getBytes());
    input = RhinoTestHelper.TestInputStream.of(updated);
    article = BaseRhinoTest.writeToLegacy(articleCrudService, createMockIngestible(articleId, input));
    byte[] updatedData = IOUtils.toByteArray(articleCrudService.readXml(articleId));
    assertEquals(updatedData, updated);
    assertArticleExistence(articleId, true);
    assertTrue(input.isClosed(), "Service didn't close stream");
  }

  @Test(dataProvider = "sampleAssets")
  public void testCreateAsset(String articleDoi, File articleFile, String assetDoi, File assetFile)
      throws IOException {
    String testAssetDoi = assetDoi.replace(articleDoi, articleDoi);

    String assetFilePath = assetFile.getPath();
    String extension = assetFilePath.substring(assetFilePath.lastIndexOf('.') + 1);
    final AssetFileIdentity assetId = AssetFileIdentity.create(testAssetDoi, extension);
    final ArticleIdentity articleId = ArticleIdentity.create(articleDoi);
    RhinoTestHelper.TestInputStream input = new RhinoTestHelper.TestFile(articleFile).read();
    input = RhinoTestHelper.alterStream(input, articleDoi, articleDoi);
    Article article = BaseRhinoTest.writeToLegacy(articleCrudService, createMockIngestible(articleId, input));

    RhinoTestHelper.TestInputStream assetFileStream = new RhinoTestHelper.TestFile(assetFile).read();
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
  public void testListDois() throws IOException {
    Article a1 = new Article();
    a1.setDoi("info:doi/10.0/test1");
    a1.seteIssn("1932-6203");
    hibernateTemplate.save(a1);

    Article a2 = new Article();
    a2.setDoi("info:doi/10.0/test2");
    a2.seteIssn(a1.geteIssn());
    hibernateTemplate.save(a2);

    Transceiver response = articleCrudService.listDois(ArticleCriteria.create(null, null, false));
    Map<?, ?> doiList = entityGson.fromJson(response.readJson(entityGson), Map.class);

    // Kludge! Extract DOI values from object bodies
    ImmutableSet<String> dois = ImmutableSet.copyOf(Collections2.transform(doiList.values(), new Function<Object, String>() {
      @Override
      public String apply(Object input) {
        return (String) ((Map) input).get("doi");
      }
    }));

    assertEquals(dois, ImmutableSet.of(a1.getDoi(), a2.getDoi()));
  }

  @Test
  public void testArticleType() throws Exception {
    String doiStub = RhinoTestHelper.SAMPLE_ARTICLES.get(0);
    ArticleIdentity articleId = ArticleIdentity.create(RhinoTestHelper.prefixed(doiStub));
    RhinoTestHelper.TestFile sampleFile = new RhinoTestHelper.TestFile(new File(
        "src/test/resources/articles/" + doiStub + ".xml"));
    String doi = articleId.getIdentifier();
    byte[] sampleData = IOUtils.toByteArray(RhinoTestHelper.alterStream(sampleFile.read(), doi, doi));
    RhinoTestHelper.TestInputStream input = RhinoTestHelper.TestInputStream.of(sampleData);
    Article article = BaseRhinoTest.writeToLegacy(articleCrudService, createMockIngestible(articleId, input));

    String json = articleCrudService.readMetadata(articleId).readJson(entityGson);
    assertTrue(json.length() > 0);
    Gson gson = new Gson();
    Map<?, ?> articleMap = gson.fromJson(json, Map.class);
    assertEquals(articleMap.get("doi"), articleId.getKey());
    assertEquals(articleMap.get("title"), article.getTitle());

    assertEquals(articleMap.get("nlmArticleType"), "research-article");

    Map<?, ?> articleType = (Map<?, ?>) articleMap.get("articleType");
    assertEquals(articleType.get("heading"), "Research Article");
    assertEquals(articleType.get("pluralHeading"), "Research Articles");
    assertEquals(articleType.get("code"), "research_article");
  }

  @Test
  public void testArticleAuthors() throws Exception {
    Map<ArticleIdentity, Article> testArticle = createTestArticle();
    ArticleIdentity articleId = testArticle.keySet().iterator().next();

    String json = articleCrudService.readAuthors(articleId).readJson(entityGson);
    assertTrue(json.length() > 0);
    Gson gson = new Gson();
    List<?> authors = gson.fromJson(json, List.class);

    assertEquals(authors.size(), 5);
    Map<?, ?> author = (Map<?, ?>) authors.get(0);
    assertEquals(author.get("fullName"), "Taha Yasseri");
    assertNotNull(author.get("corresponding"));
    List<?> affiliations = (List<?>) author.get("affiliations");
    assertEquals(affiliations.size(), 1);
    assertEquals(affiliations.get(0),
        "Department of Theoretical Physics, Budapest University of Technology and Economics, Budapest, Hungary");

    // Example of an author with two affiliations
    author = (Map<?, ?>) authors.get(3);
    assertEquals(author.get("fullName"), "Andr\u00e1s Kornai");
    assertNull(author.get("corresponding"));
    affiliations = (List<?>) author.get("affiliations");
    assertEquals(affiliations.size(), 2);
    assertEquals(affiliations.get(0),
        "Department of Theoretical Physics, Budapest University of Technology and Economics, Budapest, Hungary");
    assertEquals(affiliations.get(1),
        "Computer and Automation Research Institute, Hungarian Academy of Sciences, Budapest, Hungary");
  }

  @Test
  public void testArticleCategories() throws Exception {
    Map<ArticleIdentity, Article> testArticle = createTestArticle();
    ArticleIdentity articleId = testArticle.keySet().iterator().next();

    String json = articleCrudService.readCategories(articleId).readJson(entityGson);
    assertTrue(json.length() > 0);
    Gson gson = new Gson();
    Map<String, Double> categories = gson.fromJson(json, Map.class);

    assertEquals(categories.size(), 2);
    Iterator<String> keyIterator = categories.keySet().iterator();
    String key1 = keyIterator.next();
    Double weight1 = categories.get(key1);
    assertEquals(key1, "/TopLevel2/term2");
    assertEquals(weight1, 10d);
    String key2 = keyIterator.next();
    Double weight2 = categories.get(key2);
    assertEquals(key2, "/TopLevel1/term1");
    assertEquals(weight2, 5d);
  }

  @Test
  public void testRepopulateArticleCategories() throws Exception {
    Map<ArticleIdentity, Article> testArticle = createTestArticle();
    ArticleIdentity articleId = testArticle.keySet().iterator().next();
    Article article = testArticle.get(articleId);

    article.setCategories(new HashMap<Category, Integer>());
    assertEquals(article.getCategories().size(), 0);

    articleCrudService.repopulateCategories(articleId);

    assertTrue(article.getCategories().size() > 0);
  }

  @Test
  public void testGetRawCategories() throws Exception {
    Map<ArticleIdentity, Article> testArticle = createTestArticle();
    ArticleIdentity articleId = testArticle.keySet().iterator().next();

    String json = articleCrudService.getRawCategories(articleId).readJson(entityGson);
    assertTrue(json.length() > 0);
    Gson gson = new Gson();
    List<String> categories = gson.fromJson(json, List.class);

    assertTrue(categories.size() > 0);
    assertEquals(categories.get(0), "dummy raw term");
  }
}
