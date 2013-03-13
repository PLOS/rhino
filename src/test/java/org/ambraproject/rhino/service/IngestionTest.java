package org.ambraproject.rhino.service;


import com.google.common.base.CharMatcher;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.gson.Gson;
import org.ambraproject.models.AmbraEntity;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.models.ArticlePerson;
import org.ambraproject.models.ArticleRelationship;
import org.ambraproject.models.Category;
import org.ambraproject.models.CitedArticle;
import org.ambraproject.models.CitedArticlePerson;
import org.ambraproject.models.Journal;
import org.ambraproject.models.Syndication;
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.content.PersonName;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.test.AssertionCollector;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * This is actually a test on {@link ArticleCrudService#write} and would normally be in {@link ArticleCrudServiceTest}.
 * But because of the special way that it gathers its assertion data, I'm putting it in its own class now. Maybe it
 * should be moved into the regular test when it's ready to supplant/replace it.
 * <p/>
 * The test case data for this class live at {@code DATA_PATH}. The XML file is raw article data as it would be passed
 * to the ingester in production. The matching JSON file is a Gson dump of an {@link org.ambraproject.models.Article}
 * instance as created by the reference implementation ({@code org.ambraproject.article.service.IngesterImpl}).
 */
public class IngestionTest extends BaseRhinoTest {
  private static final Logger log = LoggerFactory.getLogger(IngestionTest.class);

  private static final File DATA_PATH = new File("src/test/resources/articles/");
  private static final File ZIP_DATA_PATH = new File("src/test/resources/articles/");
  private static final String JSON_SUFFIX = ".json";
  private static final String XML_SUFFIX = ".xml";
  private static final String ZIP_SUFFIX = ".zip";

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private Gson entityGson;

  private static FilenameFilter forSuffix(final String suffix) {
    return new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(suffix);
      }
    };
  }

  @DataProvider
  public Object[][] generatedIngestionData() {
    File[] jsonFiles = DATA_PATH.listFiles(forSuffix(JSON_SUFFIX));
    Arrays.sort(jsonFiles);
    List<Object[]> cases = Lists.newArrayListWithCapacity(jsonFiles.length);

    // Only add to the test if both the .json and .xml file exist.
    for (File jsonFile : jsonFiles) {
      String jsonFilePath = jsonFile.getPath();
      String xmlPath = jsonFilePath.substring(0, jsonFilePath.length() - JSON_SUFFIX.length()) + XML_SUFFIX;
      File xmlFile = new File(xmlPath);
      if (!xmlFile.exists()) {
        continue;
      }

      // Don't return any articles that have a zip archive.  Those will be returned
      // instead by generatedZipIngestionData() for a different test.
      String zipPath = jsonFilePath.substring(0, jsonFilePath.length() - JSON_SUFFIX.length()) + ZIP_SUFFIX;
      File zipFile = new File(zipPath);
      if (!zipFile.exists()) {
        cases.add(new Object[]{jsonFile, xmlFile});
      }
    }

    return cases.toArray(new Object[0][]);
  }

  @DataProvider
  public Object[][] generatedZipIngestionData() {
    File[] jsonFiles = ZIP_DATA_PATH.listFiles(forSuffix(JSON_SUFFIX));
    Arrays.sort(jsonFiles);
    List<Object[]> cases = Lists.newArrayListWithCapacity(jsonFiles.length);
    for (File jsonFile : jsonFiles) {
      String jsonFilePath = jsonFile.getPath();
      String zipPath = jsonFilePath.substring(0, jsonFilePath.length() - JSON_SUFFIX.length()) + ZIP_SUFFIX;
      File zipFile = new File(zipPath);
      if (zipFile.exists()) {
        cases.add(new Object[]{jsonFile, zipFile});
      }
    }
    return cases.toArray(new Object[0][]);
  }

  private Article readReferenceCase(File jsonFile) throws IOException {
    Preconditions.checkNotNull(jsonFile);
    Article article;
    Reader input = null;
    boolean threw = true;
    try {
      input = new FileReader(jsonFile);
      input = new BufferedReader(input);
      article = entityGson.fromJson(input, Article.class);
      threw = false;
    } finally {
      Closeables.close(input, threw);
    }
    createTestJournal(article.geteIssn());

    return article;
  }

  /**
   * Persist a dummy Journal object with a particular eIssn into the test environment, if it doesn't already exist.
   *
   * @param eissn the journal eIssn
   */
  private void createTestJournal(String eissn) {
    Journal journal = (Journal) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Journal.class)
            .add(Restrictions.eq("eIssn", eissn))
            .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)));
    if (journal == null) {
      journal = new Journal();
      journal.setTitle("Test Journal " + eissn);
      journal.seteIssn(eissn);
      hibernateTemplate.save(journal);
    }
  }

  @Test(dataProvider = "generatedIngestionData")
  public void testIngestion(File jsonFile, File xmlFile) throws Exception {
    final Article expected = readReferenceCase(jsonFile);
    final String caseDoi = expected.getDoi();

    Article actual = articleCrudService.write(new TestFile(xmlFile).read(),
        Optional.<ArticleIdentity>absent(), DoiBasedCrudService.WriteMode.CREATE_ONLY);
    assertTrue(actual.getID() > 0, "Article doesn't have a database ID");
    assertTrue(actual.getCreated() != null, "Article doesn't have a creation date");

    // Reload the article directly from hibernate, just to be sure.
    actual = (Article) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Article.class)
            .setFetchMode("journals", FetchMode.JOIN)
            .add(Restrictions.eq("doi", caseDoi))));
    assertNotNull(actual, "Failed to create article with expected DOI");

    AssertionCollector results = compareArticle(actual, expected, false);
    log.info("{} successes", results.getSuccessCount());
    Collection<AssertionCollector.Failure> failures = results.getFailures();
    for (AssertionCollector.Failure failure : failures) {
      log.error(failure.toString());
    }
    assertEquals(failures.size(), 0, "Mismatched Article fields for " + expected.getDoi());
  }

  @Test(dataProvider = "generatedZipIngestionData")
  public void testZipIngestion(File jsonFile, File zipFile) throws Exception {
    final Article expected = readReferenceCase(jsonFile);
    Article actual = articleCrudService.writeArchive(zipFile.getCanonicalPath(),
        Optional.<ArticleIdentity>absent(), DoiBasedCrudService.WriteMode.CREATE_ONLY);
    assertTrue(actual.getID() > 0, "Article doesn't have a database ID");
    assertTrue(actual.getCreated() != null, "Article doesn't have a creation date");

    // Reload the article directly from hibernate, just to be sure.
    actual = (Article) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Article.class)
            .setFetchMode("journals", FetchMode.JOIN)
            .add(Restrictions.eq("doi", expected.getDoi()))));
    assertNotNull(actual, "Failed to create article with expected DOI");
    AssertionCollector results = compareArticle(actual, expected, true);
    log.info("{} successes", results.getSuccessCount());

    // Do some additional comparisons that only make sense for an article ingested from an archive.
    compareArchiveFields(results, actual, expected);
    Collection<AssertionCollector.Failure> failures = results.getFailures();
    for (AssertionCollector.Failure failure : failures) {
      log.error(failure.toString());
    }
    assertEquals(failures.size(), 0, "Mismatched Article fields for " + expected.getDoi());
  }

  @Test
  public void testReingestion() throws Exception {
    createTestJournal("1932-6203");
    long start = System.currentTimeMillis();
    String zipPath = ZIP_DATA_PATH.getCanonicalPath() + File.separator + "pone.0056489.zip";
    Article first = articleCrudService.writeArchive(zipPath,
        Optional.<ArticleIdentity>absent(), DoiBasedCrudService.WriteMode.CREATE_ONLY);
    assertTrue(first.getID() > 0, "Article doesn't have a database ID");
    assertTrue(first.getCreated().getTime() >= start);

    try {
      Article second = articleCrudService.writeArchive(zipPath,
          Optional.<ArticleIdentity>absent(), DoiBasedCrudService.WriteMode.CREATE_ONLY);
      fail("Article creation succeeded for second ingestion in CREATE_ONLY mode");
    } catch (RestClientException expected) {
      assertEquals(expected.getResponseStatus(), HttpStatus.METHOD_NOT_ALLOWED);
    }

    Article second = articleCrudService.writeArchive(zipPath,
        Optional.<ArticleIdentity>absent(), DoiBasedCrudService.WriteMode.WRITE_ANY);

    // TODO: figure out how to detect that second was re-ingested.  Don't want to
    // use modification time since the test might run in less than one clock tick.
    assertTrue(first.getID() > 0, "Article doesn't have a database ID");
  }

  private static boolean compare(AssertionCollector results, Class<?> objectType, String fieldName,
                                 @Nullable Object actual, @Nullable Object expected) {
    return compare(results, objectType.getSimpleName(), fieldName, actual, expected);
  }

  /**
   * Hook for values to be massaged before passing them to the results object for comparison.
   */
  public static boolean compare(AssertionCollector results, String objectName, String fieldName,
                                @Nullable Object actual, @Nullable Object expected) {
    return results.compare(objectName, fieldName, actual, expected);
  }

  private AssertionCollector compareArticle(Article actual, Article expected,
                                            boolean assetFilesExpected) {
    AssertionCollector results = new AssertionCollector();
    compareArticleFields(results, actual, expected);
    comparePersonLists(results, Article.class, "authors", actual.getAuthors(), expected.getAuthors());
    comparePersonLists(results, Article.class, "editors", actual.getEditors(), expected.getEditors());
    compareCategorySets(results, actual.getCategories(), expected.getCategories());
    compareJournalSets(results, actual.getJournals(), expected.getJournals());
    compareRelationshipLists(results, actual.getRelatedArticles(), expected.getRelatedArticles());
    if (assetFilesExpected) {
      compareAssetsWithExpectedFiles(results, actual.getAssets(), expected.getAssets());
    } else {
      compareAssetsWithoutExpectedFiles(results, actual.getAssets(), expected.getAssets());
    }
    compareCitationLists(results, actual.getCitedArticles(), expected.getCitedArticles());
    assertSyndications(results, actual);
    return results;
  }

  /**
   * Compare two snippets of XML code, typically XML text that will be transformed into HTML.
   */
  private static boolean compareMarkupText(AssertionCollector results,
                                           Class<?> objectType, String fieldName,
                                           CharSequence actual, CharSequence expected) {
    if (actual == null || expected == null || actual.equals(expected)) {
      // If they match like this (or either is null), just compare them as-is
      return compare(results, objectType, fieldName, actual, expected);
    }

    // Else, be more permissive with XML formatting.
    // We don't mind if actual includes whitespace between tags that was missing from expected.
    // TODO: Make this fail if actual deletes whitespace that was included in expected.
    return compare(results, objectType, fieldName, massageXml(actual), massageXml(expected));
  }

  /**
   * Change XML text to an equivalent form, for comparison.
   * <p/>
   * Expands self-closing tags. Collapses whitespace; trims leading and trailing whitespace.
   * <p/>
   * Removes whitespace from between tags. This does not produce strictly equivalent XML but it is close enough for test
   * comparisons (but see the comment in {@link #compareMarkupText}).
   * <p/>
   * This should be replaced with proper use of the XML library if it gets too hairy.
   *
   * @param text XML text
   * @return "massaged" equivalent text
   */
  private static String massageXml(CharSequence text) {
    text = CharMatcher.WHITESPACE.collapseFrom(text, ' ');
    text = WHITESPACE_BETWEEN_TAGS.matcher(text).replaceAll("><");
    text = SELF_CLOSING_TAG.matcher(text).replaceAll("<$1$2></$1>");
    return text.toString();
  }

  private static final Pattern WHITESPACE_BETWEEN_TAGS = Pattern.compile(">\\s+<");
  private static final Pattern SELF_CLOSING_TAG = Pattern.compile(""
      + "<"
      + "([^>\\s]+)" // Tag name.
      + "([^>]*?)\\s*" // Attributes, if any. (Exclude trailing whitespace from captured group.)
      + "/>"
  );

  private static void compareMarkupLists(AssertionCollector results, Class<?> objectType, String fieldName,
                                         List<? extends CharSequence> actual, List<? extends CharSequence> expected) {
    // Force random access and assert no null elements
    actual = ImmutableList.copyOf(actual);
    expected = ImmutableList.copyOf(expected);

    int maxIndex = Math.max(actual.size(), expected.size());
    for (int i = 0; i < maxIndex; i++) {
      CharSequence actualElement = (i < actual.size()) ? actual.get(i) : null;
      CharSequence expectedElement = (i < expected.size()) ? expected.get(i) : null;
      compareMarkupText(results, objectType, String.format("%s[%d]", fieldName, i), actualElement, expectedElement);
    }
  }

  /**
   * Compare simple (non-associative) fields.
   *
   * @param results
   * @param actual
   * @param expected
   */
  private void compareArticleFields(AssertionCollector results, Article actual, Article expected) {
    compare(results, Article.class, "doi", actual.getDoi(), expected.getDoi());
    compareMarkupText(results, Article.class, "title", actual.getTitle(), expected.getTitle());
    compare(results, Article.class, "eIssn", actual.geteIssn(), expected.geteIssn());
    compare(results, Article.class, "state", actual.getState(), expected.getState());
    compareMarkupText(results, Article.class, "description", actual.getDescription(), expected.getDescription());
    compareMarkupText(results, Article.class, "rights", actual.getRights(), expected.getRights());
    compare(results, Article.class, "language", actual.getLanguage(), expected.getLanguage());
    compare(results, Article.class, "format", actual.getFormat(), expected.getFormat());
    compare(results, Article.class, "pages", actual.getPages(), expected.getPages());
    compare(results, Article.class, "eLocationId", actual.geteLocationId(), expected.geteLocationId());

    // actual.getDate() returns a java.sql.Date since it's coming from hibernate.  We have
    // to convert that to a java.util.Date (which GSON returns) for the comparison.
    compare(results, Article.class, "date", new Date(actual.getDate().getTime()),
        expected.getDate());
    compare(results, Article.class, "volume", actual.getVolume(), expected.getVolume());
    compare(results, Article.class, "issue", actual.getIssue(), expected.getIssue());
    compare(results, Article.class, "journal", actual.getJournal(), expected.getJournal());
    compare(results, Article.class, "publisherLocation", actual.getPublisherLocation(), expected.getPublisherLocation());
    compare(results, Article.class, "publisherName", actual.getPublisherName(), expected.getPublisherName());
    compare(results, Article.class, "url", actual.getUrl(), expected.getUrl());
    compareMarkupLists(results, Article.class, "collaborativeAuthors", actual.getCollaborativeAuthors(), expected.getCollaborativeAuthors());
    compare(results, Article.class, "types", actual.getTypes(), expected.getTypes());
  }

  private void compareCategorySets(AssertionCollector results, Set<Category> actual, Set<Category> expected) {
    /*
     * Ignore this field. We rely on an external taxonomy server to set it, which in testing will set only dummy values.
     */
  }

  private void compareJournalSets(AssertionCollector results, Set<Journal> actualSet, Set<Journal> expectedSet) {
    // We care only about eIssn, because that's the only part given in article XML.
    // All other Journal fields come from the environment, which doesn't exist here (see createTestJournal).
    Set<String> actualEissns = mapJournalsByEissn(actualSet).keySet();
    Set<String> expectedEissns = mapJournalsByEissn(expectedSet).keySet();

    for (String missing : Sets.difference(expectedEissns, actualEissns)) {
      compare(results, Journal.class, "eIssn", null, missing);
    }
    for (String extra : Sets.difference(actualEissns, expectedEissns)) {
      compare(results, Journal.class, "eIssn", extra, null);
    }
    for (String eissn : Sets.intersection(actualEissns, expectedEissns)) {
      compare(results, Journal.class, "eIssn", eissn, eissn);
    }
  }

  private void compareRelationshipLists(AssertionCollector results, List<ArticleRelationship> actual, List<ArticleRelationship> expected) {
    Map<String, ArticleRelationship> actualMap = mapRelationshipsByDoi(actual);
    Set<String> actualDois = actualMap.keySet();
    Map<String, ArticleRelationship> expectedMap = mapRelationshipsByDoi(expected);
    Set<String> expectedDois = expectedMap.keySet();

    for (String missingDoi : Sets.difference(expectedDois, actualDois)) {
      compare(results, ArticleRelationship.class, "otherArticleDoi", null, missingDoi);
    }
    for (String extraDoi : Sets.difference(actualDois, expectedDois)) {
      compare(results, ArticleRelationship.class, "otherArticleDoi", extraDoi, null);
    }

    for (String doi : Sets.intersection(actualDois, expectedDois)) {
      compare(results, ArticleRelationship.class, "otherArticleDoi", doi, doi);
      compare(results, ArticleRelationship.class, "type", actualMap.get(doi).getType(), expectedMap.get(doi).getType());
    }
  }

  private void compareAssetsWithoutExpectedFiles(AssertionCollector results,
                                                 Collection<ArticleAsset> actualList, Collection<ArticleAsset> expectedList) {
    // Compare assets by their DOI, ignoring order
    Map<AssetIdentity, ArticleAsset> actualAssetMap = mapUninitAssetsById(actualList);
    Set<AssetIdentity> actualAssetIds = actualAssetMap.keySet();
    Multimap<AssetIdentity, ArticleAsset> expectedAssetMap = mapAssetFilesByAssetId(expectedList);
    Set<AssetIdentity> expectedAssetIds = expectedAssetMap.keySet();

    for (AssetIdentity missingDoi : Sets.difference(expectedAssetIds, actualAssetIds)) {
      compare(results, ArticleAsset.class, "doi", null, missingDoi);
    }
    for (AssetIdentity extraDoi : Sets.difference(actualAssetIds, expectedAssetIds)) {
      compare(results, ArticleAsset.class, "doi", extraDoi, null);
    }

    for (AssetIdentity assetDoi : Sets.intersection(actualAssetIds, expectedAssetIds)) {
      // One created asset with a null extension and material from article XML
      ArticleAsset actualAsset = actualAssetMap.get(assetDoi);

      // Multiple assets corresponding to various uploaded files
      Collection<ArticleAsset> expectedFileAssets = expectedAssetMap.get(assetDoi);

      /*
       * The relevant fields of the expected assets should all match each other. (If a counterexample is found,
       * will have to change this test.) We want to test that the actual asset matches all of them.
       */
      verifyExpectedAssets(expectedFileAssets);
      for (ArticleAsset expectedAsset : expectedFileAssets) {
        compareAssetFields(results, actualAsset, expectedAsset, false);
      }
    }
  }

  private void compareAssetsWithExpectedFiles(AssertionCollector results,
                                              Collection<ArticleAsset> actual, Collection<ArticleAsset> expected) {
    SortAssetsReturnValue actualReturnValue = sortAssets(actual);
    List<AssetFileIdentity> actualSorted = actualReturnValue.sortedList;
    Map<AssetFileIdentity, ArticleAsset> actualAssetMap = actualReturnValue.assetMap;
    Set<AssetFileIdentity> actualSet = new HashSet<AssetFileIdentity>(actualSorted);

    SortAssetsReturnValue expectedReturnValue = sortAssets(expected);
    List<AssetFileIdentity> expectedSorted = expectedReturnValue.sortedList;
    Map<AssetFileIdentity, ArticleAsset> expectedAssetMap = expectedReturnValue.assetMap;
    Set<AssetFileIdentity> expectedSet = new HashSet<AssetFileIdentity>(expectedSorted);

    for (AssetFileIdentity missing : Sets.difference(expectedSet, actualSet)) {
      compare(results, ArticleAsset.class, "doi/extension", null, missing);
    }
    for (AssetFileIdentity extra : Sets.difference(actualSet, expectedSet)) {
      compare(results, ArticleAsset.class, "doi/extension", extra, null);
    }

    if (actualSorted.size() == expectedSorted.size()) {
      for (int i = 0; i < actualSorted.size(); i++) {
        compareAssetFields(results, actualAssetMap.get(actualSorted.get(i)),
            expectedAssetMap.get(expectedSorted.get(i)), true);
      }
    }
  }

  private static class SortAssetsReturnValue {
    List<AssetFileIdentity> sortedList;
    Map<AssetFileIdentity, ArticleAsset> assetMap;
  }

  private SortAssetsReturnValue sortAssets(Collection<ArticleAsset> assets) {
    List<AssetFileIdentity> sortedList = new ArrayList<AssetFileIdentity>(assets.size());
    Map<AssetFileIdentity, ArticleAsset> assetMap = new HashMap<AssetFileIdentity, ArticleAsset>();
    for (ArticleAsset asset : assets) {
      AssetFileIdentity afi = AssetFileIdentity.create(asset.getDoi(), asset.getExtension());
      sortedList.add(afi);
      assetMap.put(afi, asset);
    }
    Collections.sort(sortedList);
    SortAssetsReturnValue results = new SortAssetsReturnValue();
    results.sortedList = sortedList;
    results.assetMap = assetMap;
    return results;
  }

  /**
   * Compare only those fields that can be gotten from article XML.
   *
   * @param results  the object into which to insert results
   * @param actual   an actual asset with no information specific to an uploaded file
   * @param expected an expected asset with an associated file
   */
  private void compareAssetFields(AssertionCollector results, ArticleAsset actual,
                                  ArticleAsset expected, boolean assetFileExpected) {
    assertEquals(actual.getDoi(), expected.getDoi()); // should be true as a method precondition

    compare(results, ArticleAsset.class, "contextElement", actual.getContextElement(), expected.getContextElement());
    compareMarkupText(results, ArticleAsset.class, "title", actual.getTitle(), expected.getTitle());
    compareMarkupText(results, ArticleAsset.class, "description", actual.getDescription(), expected.getDescription());

    if (assetFileExpected) {
      compare(results, ArticleAsset.class, "extension", actual.getExtension(), expected.getExtension());
      compare(results, ArticleAsset.class, "contentType", actual.getContentType(), expected.getContentType());
      compare(results, ArticleAsset.class, "size", actual.getSize(), expected.getSize());
    }
  }

  /**
   * Assert that the fields checked in {@link #compareAssetFields} are the same among all expected assets with the same
   * DOI. The test expects this condition to hold about its own case data, so if it fails, halt instead of logging a
   * soft case failure.
   *
   * @param assets non-empty collection of expected assets
   */
  private void verifyExpectedAssets(Iterable<ArticleAsset> assets) {
    Iterator<ArticleAsset> iterator = assets.iterator();
    ArticleAsset first = iterator.next();
    while (iterator.hasNext()) {
      ArticleAsset next = iterator.next();
      assertTrue(Objects.equal(first.getDoi(), next.getDoi()));
      assertTrue(Objects.equal(first.getContextElement(), next.getContextElement()));
      assertTrue(Objects.equal(first.getTitle(), next.getTitle()));
      assertTrue(Objects.equal(first.getDescription(), next.getDescription()));
    }
  }

  private void compareCitationLists(AssertionCollector results,
                                    List<CitedArticle> actualList, List<CitedArticle> expectedList) {
    for (CitedArticle expectedCitation : expectedList) {
      if (expectedCitation.getKey() == null) {
        // At least one expected case has null keys. Fall back on comparing by order.
        compareCitationListsByIndex(results, actualList, expectedList);
        return;
      }
    }

    Map<String, CitedArticle> actualMap = mapCitationsByKey(actualList);
    Set<String> actualKeys = actualMap.keySet();
    Map<String, CitedArticle> expectedMap = mapCitationsByKey(expectedList);
    Set<String> expectedKeys = expectedMap.keySet();

    for (String key : Sets.intersection(actualKeys, expectedKeys)) {
      compareCitations(results, actualMap.get(key), expectedMap.get(key));
    }
    for (String extra : Sets.difference(actualKeys, expectedKeys)) {
      compare(results, Article.class, "citedArticles", actualMap.get(extra), null);
    }
    for (String missing : Sets.difference(expectedKeys, actualKeys)) {
      compare(results, Article.class, "citedArticles", null, expectedMap.get(missing));
    }
  }

  private void compareCitationListsByIndex(AssertionCollector results,
                                           List<CitedArticle> actualList, List<CitedArticle> expectedList) {
    // Ensure no problems with random access or delayed evaluation
    actualList = ImmutableList.copyOf(actualList);
    expectedList = ImmutableList.copyOf(expectedList);

    int commonSize = Math.min(actualList.size(), expectedList.size());
    for (int i = 0; i < commonSize; i++) {
      compareCitations(results, actualList.get(i), expectedList.get(i));
    }

    // If the sizes didn't match, report missing/extra citations as errors
    for (int i = commonSize; i < actualList.size(); i++) {
      compare(results, Article.class, "citedArticles", actualList.get(i), null);
    }
    for (int i = commonSize; i < expectedList.size(); i++) {
      compare(results, Article.class, "citedArticles", null, expectedList.get(i));
    }
  }

  private void compareCitations(AssertionCollector results, CitedArticle actual, CitedArticle expected) {
    if (isEmptyCitation(expected)) {
      return; // Apparently these occur because of an Admin bug. Assume the actual data is correct.
    }

    compare(results, CitedArticle.class, "key", actual.getKey(), expected.getKey());
    compare(results, CitedArticle.class, "year", actual.getYear(), expected.getYear());
    compare(results, CitedArticle.class, "displayYear", actual.getDisplayYear(), expected.getDisplayYear());
    compare(results, CitedArticle.class, "month", actual.getMonth(), expected.getMonth());
    compare(results, CitedArticle.class, "day", actual.getDay(), expected.getDay());
    compare(results, CitedArticle.class, "volumeNumber", actual.getVolumeNumber(), expected.getVolumeNumber());
    compare(results, CitedArticle.class, "volume", actual.getVolume(), expected.getVolume());
    compare(results, CitedArticle.class, "issue", actual.getIssue(), expected.getIssue());
    compareMarkupText(results, CitedArticle.class, "title", actual.getTitle(), expected.getTitle());
    compare(results, CitedArticle.class, "publisherLocation", actual.getPublisherLocation(), expected.getPublisherLocation());
    compare(results, CitedArticle.class, "publisherName", actual.getPublisherName(), expected.getPublisherName());
    compare(results, CitedArticle.class, "pages", actual.getPages(), expected.getPages());
    compare(results, CitedArticle.class, "eLocationID", actual.geteLocationID(), expected.geteLocationID());
    compare(results, CitedArticle.class, "journal", actual.getJournal(), expected.getJournal());
    compareMarkupText(results, CitedArticle.class, "note", actual.getNote(), expected.getNote());
    compareMarkupLists(results, CitedArticle.class, "collaborativeAuthors", actual.getCollaborativeAuthors(), expected.getCollaborativeAuthors());
    compare(results, CitedArticle.class, "url", actual.getUrl(), expected.getUrl());
    compare(results, CitedArticle.class, "doi", actual.getDoi(), expected.getDoi());
    compare(results, CitedArticle.class, "summary", actual.getSummary(), expected.getSummary());
    compare(results, CitedArticle.class, "citationType", actual.getCitationType(), expected.getCitationType());

    comparePersonLists(results, CitedArticle.class, "authors", actual.getAuthors(), expected.getAuthors());
    comparePersonLists(results, CitedArticle.class, "editors", actual.getEditors(), expected.getEditors());
  }

  private static boolean isEmptyCitation(CitedArticle c) {
    return c.getCitationType() == null && c.getYear() == null && c.getDisplayYear() == null && c.getMonth() == null
        && c.getDay() == null && c.getVolume() == null && c.getVolumeNumber() == null
        && c.getPublisherLocation() == null && c.getPublisherName() == null && c.getPages() == null
        && c.geteLocationID() == null && c.getJournal() == null && c.getIssue() == null && c.getUrl() == null
        && c.getDoi() == null && c.getNote() == null && c.getTitle() == null && c.getSummary() == null
        && c.getAuthors().isEmpty() && c.getEditors().isEmpty() && c.getCollaborativeAuthors().isEmpty();
  }

  private void comparePersonLists(AssertionCollector results, Class<?> parentType, String fieldName,
                                  List<? extends AmbraEntity> actualList, List<? extends AmbraEntity> expectedList) {
    final String field = parentType.getSimpleName() + "." + fieldName;

    List<PersonName> actualNames = asPersonNames(actualList);
    List<PersonName> expectedNames = asPersonNames(expectedList);

    int commonSize = Math.min(actualNames.size(), expectedNames.size());
    for (int i = 0; i < commonSize; i++) {
      PersonName actualName = actualNames.get(i);
      PersonName expectedName = expectedNames.get(i);

      compare(results, field, "fullName", actualName.getFullName(), expectedName.getFullName());
      compare(results, field, "givenNames", actualName.getGivenNames(), expectedName.getGivenNames());
      compare(results, field, "surname", actualName.getSurname(), expectedName.getSurname());
      compare(results, field, "suffix", actualName.getSuffix(), expectedName.getSuffix());
    }

    // If the sizes didn't match, report missing/extra elements as errors
    for (int i = commonSize; i < actualList.size(); i++) {
      compare(results, parentType, fieldName, actualNames.get(i), null);
    }
    for (int i = commonSize; i < expectedList.size(); i++) {
      compare(results, parentType, fieldName, null, expectedNames.get(i));
    }
  }

  /**
   * Tests some Article fields that are only populated if we ingest a .zip archive.
   */
  private void compareArchiveFields(AssertionCollector results, Article actual, Article expected) {
    compare(results, Article.class, "archiveName", actual.getArchiveName(),
        expected.getArchiveName());
    compare(results, Article.class, "strkImgURI", Strings.nullToEmpty(actual.getStrkImgURI()),
        Strings.nullToEmpty(expected.getStrkImgURI()));
  }

  private Syndication buildExpectedSyndication(String target, Article article) {
    Syndication result = new Syndication();
    result.setDoi(article.getDoi());
    result.setTarget(target);
    result.setStatus("PENDING");
    result.setSubmissionCount(0);
    return result;
  }

  private void assertSyndications(AssertionCollector results, Article article) {

    // There is no getter for syndication in article, since the foreign key is
    // doi instead of articleID.  So we can't do the comparison via JSON as we
    // do elsewhere in this test.
    List<Syndication> expected = new ArrayList<Syndication>(2);
    expected.add(buildExpectedSyndication("CROSSREF", article));
    expected.add(buildExpectedSyndication("PMC", article));
    List<Syndication> actual = hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Syndication.class)
            .add(Restrictions.eq("doi", article.getDoi()))
            .addOrder(Order.asc("target")));

    int commonSize = Math.min(expected.size(), actual.size());
    for (int i = 0; i < commonSize; i++) {
      results.compare(Syndication.class, "syndication", actual.get(i), expected.get(i));
    }
    for (int i = commonSize; i < actual.size(); i++) {
      results.compare(Syndication.class, "syndication", actual.get(i), null);
    }
    for (int i = commonSize; i < expected.size(); i++) {
      results.compare(Syndication.class, "syndication", null, expected.get(i));
    }
  }

  // Transformation helper methods

  private static ImmutableMap<String, Journal> mapJournalsByEissn(Collection<Journal> journals) {
    ImmutableMap.Builder<String, Journal> map = ImmutableMap.builder();
    for (Journal journal : journals) {
      map.put(journal.geteIssn(), journal);
    }
    return map.build();
  }

  private static ImmutableMap<String, ArticleRelationship> mapRelationshipsByDoi(Collection<ArticleRelationship> relationships) {
    ImmutableMap.Builder<String, ArticleRelationship> map = ImmutableMap.builder();
    for (ArticleRelationship relationship : relationships) {
      map.put(relationship.getOtherArticleDoi(), relationship);
    }
    return map.build();
  }

  private static ImmutableMap<AssetIdentity, ArticleAsset> mapUninitAssetsById(Collection<ArticleAsset> assets) {
    ImmutableMap.Builder<AssetIdentity, ArticleAsset> map = ImmutableMap.builder();
    for (ArticleAsset asset : assets) {
      map.put(AssetIdentity.from(asset), asset);
    }
    return map.build();
  }

  private static ImmutableMultimap<AssetIdentity, ArticleAsset> mapAssetFilesByAssetId(Collection<ArticleAsset> assets) {
    ImmutableListMultimap.Builder<AssetIdentity, ArticleAsset> map = ImmutableListMultimap.builder();
    for (ArticleAsset asset : assets) {
      AssetFileIdentity fileIdentity = AssetFileIdentity.from(asset);
      map.put(fileIdentity.forAsset(), asset);
    }
    return map.build();
  }

  private static ImmutableMap<String, CitedArticle> mapCitationsByKey(Collection<CitedArticle> citations) {
    ImmutableMap.Builder<String, CitedArticle> map = ImmutableMap.builder();
    for (CitedArticle citation : citations) {
      map.put(citation.getKey(), citation);
    }
    return map.build();
  }

  private static ImmutableList<PersonName> asPersonNames(Collection<? extends AmbraEntity> persons) {
    List<PersonName> names = Lists.newArrayListWithCapacity(persons.size());
    for (Object personObj : persons) {
      // Have to do it this way for the same reason that PersonName exists in the first place -- see PersonName docs
      PersonName name;
      if (personObj instanceof ArticlePerson) {
        name = PersonName.from((ArticlePerson) personObj);
      } else if (personObj instanceof CitedArticlePerson) {
        name = PersonName.from((CitedArticlePerson) personObj);
      } else {
        throw new ClassCastException();
      }
      names.add(name);
    }
    return ImmutableList.copyOf(names);
  }

}
