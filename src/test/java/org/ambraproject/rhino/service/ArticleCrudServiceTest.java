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
package org.ambraproject.rhino.service;

import org.ambraproject.rhino.BaseRhinoTransactionalTest;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.service.impl.IngestionService;
import org.plos.crepo.service.ContentRepoService;
import org.plos.crepo.service.InMemoryContentRepoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;

public class ArticleCrudServiceTest extends BaseRhinoTransactionalTest {

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private AssetCrudService assetCrudService;
  @Autowired
  private ContentRepoService contentRepoService;
  @Autowired
  private IngestionService ingestionService;

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

//  private void assertArticleExistence(ArticleIdentity id, boolean expectedToExist) {
//    boolean received404 = false;
//    try {
//      articleCrudService.readXml(id);
//    } catch (InMemoryContentRepoService.InMemoryContentRepoServiceException|NotFoundException nfe) {
//      received404 = true;
//    } catch (RestClientException e) {
//      throw e;
//    }
//    assertEquals(received404, !expectedToExist,
//        (expectedToExist ? "Article expected to exist but doesn't" : "Article expected not to exist but does"));
//  }

  private void assertGoodText(String text) {
    assertNotNull(text, "Text field was not set");
    assertFalse(text.isEmpty(), "Text field was set to an empty string");
    assertFalse(Character.isWhitespace(text.charAt(0)), "Text field was set with leading whitespace");
    assertFalse(Character.isWhitespace(text.charAt(text.length() - 1)),
        "Text field was set with trailing whitespace");
  }

  @Test(dataProvider = "sampleArticles", enabled = false)
  public void testCrud(String doi, File fileLocation, File referenceLocation) throws IOException {
//    final ArticleIdentity articleId = ArticleIdentity.create(doi);
//    final String key = articleId.getKey();
//
//    final RhinoTestHelper.TestFile sampleFile = new RhinoTestHelper.TestFile(fileLocation);
//    final byte[] sampleData = IOUtils.toByteArray(RhinoTestHelper.alterStream(sampleFile.read(), doi, doi));
//
//    assertArticleExistence(articleId, false);
//
//    RhinoTestHelper.TestInputStream input = RhinoTestHelper.TestInputStream.of(sampleData);
//    List<ArticleAsset> referenceAssets = RhinoTestHelper.readReferenceCase(referenceLocation).getAssets();
//    Archive mockIngestible = RhinoTestHelper.createMockIngestible(articleId, input, referenceAssets);
//    ingestionService.ingest(mockIngestible);
//    assertArticleExistence(articleId, true);
//    assertTrue(input.isClosed(), "Service didn't close stream");
//
//    Article stored = (Article) DataAccessUtils.uniqueResult((List<?>)
//        hibernateTemplate.findByCriteria(DetachedCriteria
//                .forClass(Article.class)
//                .add(Restrictions.eq("doi", key))
//                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
//        ));
//    assertNotNull(stored, "ArticleCrudService.create did not store an article");
//    assertEquals(stored.getDoi(), key);
//    assertEquals(stored.getLanguage(), "en");
//    assertGoodText(stored.getDescription());
//    assertGoodText(stored.getRights());
//
//    List<ArticleAuthor> storedAuthors = stored.getAuthors();
//    assertNotNull(storedAuthors, "Article's authors field was not set");
//    assertFalse(storedAuthors.isEmpty(), "No authors were associated with the article");
//
//    // Check that no author names were stored redundantly
//    Set<String> authorNames = Sets.newHashSetWithExpectedSize(storedAuthors.size());
//    for (ArticleAuthor author : storedAuthors) {
//      String fullName = author.getFullName();
//      assertFalse(StringUtils.isBlank(fullName), "Name not set for author");
//      assertTrue(authorNames.add(fullName), "Redundant author name");
//    }
//
//    Set<Category> expectedCategories = new HashSet<>();
//    for (WeightedTerm categoryPath : DummyTaxonomyClassificationService.DUMMY_DATA) {
//      Category category = new Category();
//      category.setPath(categoryPath.getPath());
//      expectedCategories.add(category);
//    }
//    Set<Category> actualCategories = stored.getCategories().keySet();
//
//    // org.hibernate.collection.AbstractPersistentCollection.SetProxy does not respect the Set.equals contract,
//    // so copy actualCategories out to a well-behaved Set before comparing.
//    actualCategories = new HashSet<>(actualCategories);
//    assertEquals(actualCategories, expectedCategories);
//
//    byte[] readData = IOUtils.toByteArray(articleCrudService.readXml(articleId));
//    assertEquals(readData, sampleData);
//
//    final byte[] updated = Bytes.concat(sampleData, "\n<!-- Appended -->".getBytes());
//    input = RhinoTestHelper.TestInputStream.of(updated);
//    mockIngestible = RhinoTestHelper.createMockIngestible(articleId, input, referenceAssets);
//    ingestionService.ingest(mockIngestible);
//    byte[] updatedData = IOUtils.toByteArray(articleCrudService.readXml(articleId));
//    assertEquals(updatedData, updated);
//    assertArticleExistence(articleId, true);
//    assertTrue(input.isClosed(), "Service didn't close stream");

    //articleCrudService.delete(articleId);
    //assertArticleExistence(articleId, false);
  }

  @Test
  public void testListDois() throws IOException {
    Article a1 = new Article();
    a1.setDoi("info:doi/10.0/test1");
//    a1.seteIssn("1932-6203");
//    hibernateTemplate.save(a1);
//
//    Article a2 = new Article();
//    a2.setDoi("info:doi/10.0/test2");
//    a2.seteIssn(a1.geteIssn());
//    hibernateTemplate.save(a2);
//
//    Transceiver response = articleCrudService.listDois(ArticleCriteria.create(null, null, false));
//    Map<?, ?> doiList = entityGson.fromJson(response.readJson(entityGson), Map.class);
//
//    // Kludge! Extract DOI values from object bodies
//    ImmutableSet<String> dois = ImmutableSet.copyOf(Collections2.transform(doiList.values(), new Function<Object, String>() {
//      @Override
//      public String apply(Object input) {
//        return (String) ((Map) input).get("doi");
//      }
//    }));
//
//    assertEquals(dois, ImmutableSet.of(a1.getDoi(), a2.getDoi()));
  }

  @Test(enabled = false)
  public void testArticleType() throws Exception {
    Article article = new Article();//RhinoTestHelper.createTestArticle(articleCrudService);
//    ArticleIdentity articleId = ArticleIdentity.create(article);
//
//    String json = articleCrudService.serveMetadata(articleId, true).readJson(entityGson);
//    assertTrue(json.length() > 0);
//    Gson gson = new Gson();
//    Map<?, ?> articleMap = gson.fromJson(json, Map.class);
//    assertEquals(articleMap.get("doi"), articleId.getKey());
//    assertEquals(articleMap.get("title"), article.getTitle());
//
//    assertEquals(articleMap.get("nlmArticleType"), "research-article");
//
//    Map<?, ?> articleType = (Map<?, ?>) articleMap.get("articleType");
//    assertEquals(articleType.get("heading"), "Research Article");
//    assertEquals(articleType.get("pluralHeading"), "Research Articles");
//    assertEquals(articleType.get("code"), "research_article");
  }

  @Test(enabled = false)
  public void testArticleAuthors() throws Exception {
//    ArticleIdentity articleId = ArticleIdentity.create(""); //ArticleIdentity.create(RhinoTestHelper.createTestArticle(articleCrudService));
//
//    //todo: fix or remove
//    String json = ""; //articleCrudService.readAuthors(articleId).readJson(entityGson);
//    assertTrue(json.length() > 0);
//    Gson gson = new Gson();
//    Map<String, ?> authorMetadata = gson.fromJson(json, Map.class);
//    List<?> authors = (List<?>) authorMetadata.get("authors");
//
//    assertEquals(authors.size(), 5);
//    Map<?, ?> author = (Map<?, ?>) authors.get(0);
//    assertEquals(author.get("fullName"), "Taha Yasseri");
//    assertNotNull(author.get("corresponding"));
//    List<?> affiliations = (List<?>) author.get("affiliations");
//    assertEquals(affiliations.size(), 1);
//    assertEquals(affiliations.get(0),
//        "Department of Theoretical Physics, Budapest University of Technology and Economics, Budapest, Hungary");
//
//    // Example of an author with two affiliations
//    author = (Map<?, ?>) authors.get(3);
//    assertEquals(author.get("fullName"), "Andr\u00e1s Kornai");
//    assertNull(author.get("corresponding"));
//    affiliations = (List<?>) author.get("affiliations");
//    assertEquals(affiliations.size(), 2);
//    assertEquals(affiliations.get(0),
//        "Department of Theoretical Physics, Budapest University of Technology and Economics, Budapest, Hungary");
//    assertEquals(affiliations.get(1),
//        "Computer and Automation Research Institute, Hungarian Academy of Sciences, Budapest, Hungary");
  }

  @Test(enabled = false)
  public void testArticleCategories() throws Exception {
//    Article testArticle = new Article(); //RhinoTestHelper.createTestArticle(articleCrudService);
//    ArticleIdentity articleId = ArticleIdentity.create(testArticle);
//
//    String json = "";
//    //todo: fix or remove along with similar lines below
//    //String json = articleCrudService.readCategories(articleId).readJson(entityGson);
//    assertTrue(json.length() > 0);
//    Gson gson = new Gson();
//    Map<String, Double> categories = gson.fromJson(json, Map.class);
//
//    assertEquals(categories, ImmutableMap.builder()
//        .put("/TopLevel1/term1", 5d)
//        .put("/TopLevel2/term2", 10d)
//        .build());
  }

  @Test(enabled = false)
  public void testRepopulateArticleCategories() throws Exception {
//    Article article = new Article(); //RhinoTestHelper.createTestArticle(articleCrudService);
//    ArticleIdentity articleId = ArticleIdentity.create(article);
//
//    article.setCategories(new HashMap<>());
//    assertEquals(article.getCategories().size(), 0);
//
//    //articleCrudService.populateCategories(articleId); //todo: fix or remove
//
//    assertTrue(article.getCategories().size() > 0);
  }

  @Test(enabled = false)
  public void testGetRawCategories() throws Exception {
//    ArticleIdentity articleId = ArticleIdentity.create(""); //ArticleIdentity.create(RhinoTestHelper.createTestArticle(articleCrudService));
//
//    String json = "";
////    String json = articleCrudService.getRawCategories(articleId).readJson(entityGson);
//    assertTrue(json.length() > 0);
//    Gson gson = new Gson();
//    List<String> categories = gson.fromJson(json, List.class);
//
//    assertTrue(categories.size() > 0);
//    assertEquals(categories.get(0), "dummy raw term");
  }

  @Test(enabled = false)
  public void testGetRawCategoriesAndText() throws Exception {
//    ArticleIdentity articleId = ArticleIdentity.create(""); //ArticleIdentity.create(RhinoTestHelper.createTestArticle(articleCrudService));

//    String response = articleCrudService.getRawCategoriesAndText(articleId);
//    assertTrue(response.length() > 0);
//    assertEquals(response, "<pre>dummy text sent to MAIstro\n\ndummy raw term</pre>");
  }

  @Test(enabled = false)
  public void testGetPublicationJournal() throws Exception {
    Article article = new Article(); //RhinoTestHelper.createTestArticle(articleCrudService);
//    ArticleIdentity articleId = ArticleIdentity.create(article);

//    Journal journal = articleCrudService.getPublicationJournal(article);
//    assertEquals(journal.getTitle(), "Test Journal 1932-6203");
  }
}
