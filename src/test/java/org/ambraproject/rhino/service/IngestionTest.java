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

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
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
import org.ambraproject.rhino.IngestibleUtil;
import org.ambraproject.rhino.RhinoTestHelper;
import org.ambraproject.rhino.content.PersonName;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.test.AssertionCollector;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.rhino.util.StringReplacer;
import org.ambraproject.rhino.util.response.Transceiver;
import org.apache.commons.lang.StringUtils;
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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Special test suite for testing ingestion features, using the legacy Admin app as a reference implementation.
 * <p/>
 * The test case data for this class live at {@code DATA_PATH}. Each XML file is raw article data to be passed to an
 * {@link ArticleCrudService}. The matching JSON file is a Gson dump of an {@link org.ambraproject.models.Article}
 * instance as created by the reference implementation ({@code org.ambraproject.article.service.IngesterImpl}). There
 * are also full zip archives that are analogous to the XML, but test {@link IngestibleService} instead.
 * <p/>
 * The code in this class was centered around the reference implementation, and was written with the assumption that it
 * would use the unaltered JSON code for comparisons, bugs and all. Hence, there are a lot of little hacks in the
 * assertions that either relax the requirements for equality between actual and "expected" values, or outright ignore
 * certain values in the presence of known bugs. These should be well-commented.
 * <p/>
 * Better, less hackish tests are desirable in the unit test classes that explicitly test particular services.
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

  /**
   * Test cases that we want to exclude from normal runs.
   * <p/>
   * Each of these cases has been found to fail because it asserts "pathological" behavior from the old Admin app that
   * we don't want to reproduce. The input files are left in the test harness, but are skipped if they are named here.
   * The pathological test failures may be observed by commenting out an entry below. Each entry in the list should be
   * accompanied by an explanatory comment.
   */
  private static final ImmutableSet<String> TEST_CASE_EXCLUSIONS = ImmutableSet.copyOf(new String[]{

      /*
       * Asset-like files named "10.1371/journal.pbio.0020365.t001-M", "*.t002-M", and "*.t003-M" appeared in the
       * original zip file, which caused them to be mentioned in the JSON assertion, even though no such identifiers
       * appear anywhere in the article XML.
       */
      "pbio.0020365",

      /*
       * The original zip file contained a file with suffix ".xml.orig", which caused old Admin to create an unwanted
       * asset named "info:doi/10.1371/journal.pone.0027190.xml" with extension "ORIG".
       */
      "pone.0027190",

      /*
       * In old Admin, DTD validation (at some point in XML parsing) adds the attribute orientation="portrait" to the
       * XML node for 10.1371/journal.pone.0021661.e002. The e002 asset is itself nested in the caption for
       * 10.1371/journal.pone.0021661.s001. So, the check of the s001's description field fails because Rhino does no
       * online validation and never adds orientation="portrait".
       *
       * This is a questionable exclusion because we probably should be applying DTD validation (and getting the
       * consequent changes in output) anyway, using locally stored copies of the DTD files if necessary.
       * See org.ambraproject.rhino.service.impl.AmbraService.newDocumentBuilder.
       *
       * TODO: Re-examine DTD validation; try to make this case pass and delete the exclusion.
       */
      "pone.0021661",

  });

  private static FilenameFilter forSuffix(final String suffix) {
    return new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(suffix);
      }
    };
  }

  /**
   * Provide test cases for ingestion tests.
   *
   * @param assertionSuffix the file name suffix for files that assert expected behavior
   * @param testInputSuffix the file name suffix for files to use as test input
   * @param testDataPath    the path to the directory containing both above kinds of file
   * @return a list containing arrays ready to pass as @Test parameters
   */
  private List<Object[]> provideIngestionCases(String assertionSuffix, String testInputSuffix, File testDataPath) {
    File[] assertionFiles = testDataPath.listFiles(forSuffix(assertionSuffix));
    Arrays.sort(assertionFiles);
    List<Object[]> cases = Lists.newLinkedList(); // LinkedList for mid-iteration removal -- see generatedIngestionData

    // Only add to the test if files are found for both the input and asserted output
    for (File assertionFile : assertionFiles) {
      String assertionFilePath = assertionFile.getPath();
      String filePath = assertionFilePath.substring(0, assertionFilePath.length() - assertionSuffix.length());
      if (TEST_CASE_EXCLUSIONS.contains(filePath.substring(filePath.lastIndexOf('/') + 1))) {
        continue;
      }
      File testInputFile = new File(filePath + testInputSuffix);
      if (testInputFile.exists()) {
        cases.add(new Object[]{assertionFile, testInputFile});
      }
    }

    return cases;
  }

  @DataProvider
  public Object[][] generatedIngestionData() {
    List<Object[]> cases = provideIngestionCases(JSON_SUFFIX, XML_SUFFIX, DATA_PATH);

    for (Iterator<Object[]> iterator = cases.iterator(); iterator.hasNext(); ) {
      Object[] testCase = iterator.next();
      String jsonFilePath = ((File) testCase[0]).getPath();
      String filePath = jsonFilePath.substring(0, jsonFilePath.length() - JSON_SUFFIX.length());

      // Don't return any articles that have a zip archive.  Those will be returned
      // instead by generatedZipIngestionData() for a different test.
      String zipPath = filePath + ZIP_SUFFIX;
      File zipFile = new File(zipPath);
      if (zipFile.exists()) {
        iterator.remove();
      }
    }

    return cases.toArray(new Object[0][]);
  }

  @DataProvider
  public Object[][] generatedZipIngestionData() {
    return provideIngestionCases(JSON_SUFFIX, ZIP_SUFFIX, ZIP_DATA_PATH).toArray(new Object[0][]);
  }

  private Article readReferenceCase(File jsonFile) throws IOException {
    Preconditions.checkNotNull(jsonFile);
    Article article;
    try (Reader input = new BufferedReader(new FileReader(jsonFile))) {
      article = entityGson.fromJson(input, Article.class);
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
      journal = RhinoTestHelper.createDummyJournal(eissn);
      hibernateTemplate.save(journal);
    }
  }

  @Test(dataProvider = "generatedIngestionData")
  public void testIngestion(File jsonFile, File xmlFile) throws Exception {
    final Article expected = readReferenceCase(jsonFile);
    final String caseDoi = expected.getDoi();

    RhinoTestHelper.TestInputStream testInputStream = new RhinoTestHelper.TestFile(xmlFile).read();
    Archive ingestible = Archive.readZipFileIntoMemory(xmlFile.getName() + ".zip",
        IngestibleUtil.buildMockIngestible(testInputStream));
    Article actual = writeToLegacy(articleCrudService, ingestible);
    assertTrue(actual.getID() > 0, "Article doesn't have a database ID");
    assertTrue(actual.getCreated() != null, "Article doesn't have a creation date");

    // Reload the article directly from hibernate, just to be sure.
    actual = (Article) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Article.class)
            .setFetchMode("journals", FetchMode.JOIN)
            .setFetchMode("journals.volumes", FetchMode.JOIN)
            .setFetchMode("journals.articleList", FetchMode.JOIN)
            .add(Restrictions.eq("doi", caseDoi))));
    assertNotNull(actual, "Failed to create article with expected DOI");

    AssertionCollector results = compareArticle(actual, expected, false);
    log.info("{} successes", results.getSuccessCount());
    Collection<AssertionCollector.Failure> failures = results.getFailures();
    for (AssertionCollector.Failure failure : failures) {
      log.error(failure.toString());
    }
    assertEquals(failures.size(), 0, "Mismatched Article fields for " + expected.getDoi());
    testReadMetadata(actual);
  }

  private void testReadMetadata(Article article) throws IOException {
    // Mostly we want to test that this method call doesn't crash or hang
    Transceiver response = articleCrudService.readMetadata(article, true);

    assertFalse(StringUtils.isBlank(response.readJson(entityGson)));
  }

  @Test(dataProvider = "generatedZipIngestionData")
  public void testZipIngestion(File jsonFile, File zipFile) throws Exception {
    final Article expected = readReferenceCase(jsonFile);
    Article actual = writeToLegacy(articleCrudService, Archive.readZipFileIntoMemory(zipFile));
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
    Archive zipPath = Archive.readZipFileIntoMemory(new File(
        ZIP_DATA_PATH.getCanonicalPath() + File.separator + "pone.0056489.zip"));
    Article first = writeToLegacy(articleCrudService, zipPath);
    assertTrue(first.getID() > 0, "Article doesn't have a database ID");
    assertTrue(first.getCreated().getTime() >= start);

    Article second = writeToLegacy(articleCrudService, zipPath);

    // TODO: figure out how to detect that second was re-ingested.  Don't want to
    // use modification time since the test might run in less than one clock tick.
    assertTrue(first.getID() > 0, "Article doesn't have a database ID");
  }

  /**
   * Tests ingestion of an intentionally bad .zip file to confirm that all article entities are deleted after the
   * error.
   */
  @Test
  public void testArchiveError() throws Exception {
    createTestJournal("1932-6203");

    // An intentionally-constructed bad zip file that contains asset files not referenced
    // in the XML.  (This seems like the easiest way to make ingestion blow up when
    // processing an asset.)
    String zipPath = ZIP_DATA_PATH.getCanonicalPath() + File.separator + "bad_zips"
        + File.separator + "pone.0060593.zip";
    try {
      Article article = writeToLegacy(articleCrudService, Archive.readZipFileIntoMemory(new File(zipPath)));
      fail("Ingesting bad zip did not throw exception");
    } catch (RestClientException expected) {
      assertEquals(expected.getResponseStatus(), HttpStatus.METHOD_NOT_ALLOWED);
    }

    List<Article> articles = hibernateTemplate.findByCriteria(DetachedCriteria
        .forClass(Article.class)
        .add(Restrictions.eq("doi", "info:doi/10.1371/journal.pone.0060593")));
    assertEquals(articles.size(), 0, "Bad zip archive left a row in article!");
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
    if (expected instanceof CharSequence) {
      expected = CharMatcher.WHITESPACE.collapseFrom((CharSequence) expected, ' ');
    }
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

    // Since citedArticles are lazily loaded, and this test doesn't currently execute transactionally,
    // we have to reload the actual citations separately.
    // TODO: fix this.
    List<CitedArticle> actualCitations = hibernateTemplate.find("FROM CitedArticle WHERE articleID = ?",
        actual.getID());
    compareCitationLists(results, actualCitations, expected.getCitedArticles());
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
    actual = massageXml(actual);
    expected = massageXml(expected);
    if (!actual.equals(expected) && optionalWhitespace(actual).matcher(expected).matches()) {
      /*
       * Old Admin incorrectly ignores line breaks, therefore omitting whitespace. So 'actual' may still be correct if
       * it has a whitespace group that 'expected' omits entirely. This means the test might fail to catch bugs where
       * 'actual' is inserting whitespace where it actually shouldn't be (such as in the middle of a word), but that is
       * relatively low-risk versus known cases where 'expected' reflects buggy behavior.
       *
       * See the pone.0040470 case for examples.
       */
      expected = actual;
    }
    return compare(results, objectType, fieldName, actual, expected);
  }

  /**
   * Change XML text to an equivalent form, for comparison.
   * <p/>
   * Expands self-closing tags. Collapses whitespace; trims leading and trailing whitespace.
   * <p/>
   * Removes whitespace from between tags. This does not produce strictly equivalent XML but it is close enough for test
   * comparisons (but see the comment in {@link #compareMarkupText}).
   * <p/>
   * Un-escape ampersands where they appear within an attribute value. (This could be handled more generally, but it
   * barely comes up. Legacy Admin unnecessarily escapes ampersands in this context on case pone.0005668.)
   * <p/>
   * This should be replaced with proper use of the XML library if it gets too hairy.
   *
   * @param text XML text
   * @return "massaged" equivalent text
   */
  private static String massageXml(CharSequence text) {
    text = CharMatcher.WHITESPACE.collapseFrom(text, ' ');
    return XML_MASSAGER.replace(text);
  }

  private static final StringReplacer XML_MASSAGER = StringReplacer.builder()
      .replaceRegex(">\\s+<", "><") // Whitespace between tags
      .replaceRegex("" // Self-closing tags
              + "<"
              + "([^>\\s]+)"   // Tag name.
              + "([^>]*?)\\s*" // Attributes, if any. (Exclude trailing whitespace from captured group.)
              + "/>",
          "<$1$2></$1>"
      )
      .replaceRegex("" // Ampersands escaped within a tag attribute
              + "(<[^>]*"    // Stuff in the tag before the attribute begins
              + "\"[^>\"]*)" // Stuff in the attribute before the escaped ampersand
              + "&amp;"      // The escaped ampersand
              + "([^>\"]*\"" // Closes the attribute
              + "[^>]*>)",   // Closes the tag
          "$1&$2"
      )
      .build();

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

  private static final Pattern NON_WHITESPACE = Pattern.compile("[^\\s]+");

  /**
   * Produce a pattern that permits whitespace to be omitted or substituted with other whitespace.
   *
   * @param text
   * @return
   */
  private static Pattern optionalWhitespace(CharSequence text) {
    Matcher nonWhitespaceGroups = NON_WHITESPACE.matcher(Preconditions.checkNotNull(text));
    StringBuilder optionalWhitespacePattern = new StringBuilder(text.length());
    while (nonWhitespaceGroups.find()) {
      if (optionalWhitespacePattern.length() > 0) {
        optionalWhitespacePattern.append("\\s*");
      }
      optionalWhitespacePattern.append(Pattern.quote(nonWhitespaceGroups.group()));
    }
    return Pattern.compile(optionalWhitespacePattern.toString());
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
    compareRights(results, actual.getRights(), expected.getRights());
    compare(results, Article.class, "language", actual.getLanguage(), expected.getLanguage());
    compare(results, Article.class, "format", actual.getFormat(), expected.getFormat());
    comparePageRanges(results, Article.class, "pages", actual.getPages(), expected.getPages());
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

  private boolean compareRights(AssertionCollector results, String actualRights, String expectedRights) {
    final String unwantedPrefix = ". "; // Legacy bug: this appears before license if no holder is provided.
    if (expectedRights != null && expectedRights.startsWith(unwantedPrefix)) {
      expectedRights = expectedRights.substring(unwantedPrefix.length());
    }
    return compare(results, Article.class, "rights", actualRights, expectedRights);
  }

  private void compareCategorySets(AssertionCollector results, Map<Category, Integer> actual, Map<Category, Integer> expected) {
    /*
     * Ignore this field. We rely on an external taxonomy server to set it, which in testing will set only dummy values.
     */
  }

  private void compareJournalSets(AssertionCollector results, Set<Journal> actualSet, Set<Journal> expectedSet) {
    // We care only about eIssn, because that's the only part given in article XML.
    // All other Journal fields come from the environment, which doesn't exist here (see createTestJournal).
    Set<String> actualEissns = Maps.uniqueIndex(actualSet, JOURNAL_EISSN).keySet();
    Set<String> expectedEissns = Maps.uniqueIndex(expectedSet, JOURNAL_EISSN).keySet();

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
    Map<String, ArticleRelationship> actualMap = Maps.uniqueIndex(actual, RELATIONSHIP_DOI);
    Set<String> actualDois = actualMap.keySet();
    Map<String, ArticleRelationship> expectedMap = Maps.uniqueIndex(expected, RELATIONSHIP_DOI);
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
    Map<AssetIdentity, ArticleAsset> actualAssetMap = Maps.uniqueIndex(actualList, ASSET_IDENTITY);
    Set<AssetIdentity> actualAssetIds = actualAssetMap.keySet();
    Multimap<AssetIdentity, ArticleAsset> expectedAssetMap = Multimaps.index(expectedList, ASSET_IDENTITY);
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
    Map<AssetFileIdentity, ArticleAsset> actualMap = Maps.uniqueIndex(actual, ASSET_FILE_IDENTITY);
    Set<AssetFileIdentity> actualKeys = actualMap.keySet();
    Map<AssetFileIdentity, ArticleAsset> expectedMap = Maps.uniqueIndex(expected, ASSET_FILE_IDENTITY);
    Set<AssetFileIdentity> expectedKeys = expectedMap.keySet();

    for (AssetFileIdentity missing : Sets.difference(expectedKeys, actualKeys)) {
      compare(results, ArticleAsset.class, "doi/extension", null, missing);
    }
    for (AssetFileIdentity extra : Sets.difference(actualKeys, expectedKeys)) {
      compare(results, ArticleAsset.class, "doi/extension", extra, null);
    }
    for (AssetFileIdentity key : Sets.intersection(actualKeys, expectedKeys)) {
      compareAssetFields(results, actualMap.get(key), expectedMap.get(key), true);
    }
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

    String actualContextElement = actual.getContextElement();
    String expectedContextElement = expected.getContextElement();
    if ("".equals(expectedContextElement) && "inline-formula".equals(actualContextElement)) {
      /*
       * Some test cases have an empty string for inline-formula elements. This is not true for all inline formulas,
       * therefore this seems not to be a special case where the empty string is correct. It's probably a legacy bug.
       * So, make sure we don't assert for the buggy value.
       */
      expectedContextElement = actualContextElement;
    }
    compare(results, ArticleAsset.class, "contextElement(" + actual.getDoi() + ")", actualContextElement, expectedContextElement);

    compareMarkupText(results, ArticleAsset.class, "title(" + actual.getDoi() + ")", actual.getTitle(), expected.getTitle());
    compareMarkupText(results, ArticleAsset.class, "description(" + actual.getDoi() + ")", actual.getDescription(), expected.getDescription());

    if (assetFileExpected) {
      compare(results, ArticleAsset.class, "extension(" + actual.getDoi() + ")", actual.getExtension(), expected.getExtension());
      compare(results, ArticleAsset.class, "contentType(" + actual.getDoi() + ")", actual.getContentType(), expected.getContentType());
      compare(results, ArticleAsset.class, "size(" + actual.getDoi() + ")", actual.getSize(), expected.getSize());
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

    Map<String, CitedArticle> actualMap = Maps.uniqueIndex(actualList, CITATION_KEY);
    Set<String> actualKeys = actualMap.keySet();
    Map<String, CitedArticle> expectedMap = Maps.uniqueIndex(expectedList, CITATION_KEY);
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
    compareYear(results, actual.getYear(), expected.getYear());
    compare(results, CitedArticle.class, "displayYear", actual.getDisplayYear(), expected.getDisplayYear());
    compare(results, CitedArticle.class, "month", actual.getMonth(), expected.getMonth());
    compare(results, CitedArticle.class, "day", actual.getDay(), expected.getDay());
    compare(results, CitedArticle.class, "volumeNumber", actual.getVolumeNumber(), expected.getVolumeNumber());
    compare(results, CitedArticle.class, "volume", actual.getVolume(), expected.getVolume());
    compare(results, CitedArticle.class, "issue", actual.getIssue(), expected.getIssue());
    compareMarkupText(results, CitedArticle.class, "title", actual.getTitle(), expected.getTitle());
    compare(results, CitedArticle.class, "publisherLocation", actual.getPublisherLocation(), expected.getPublisherLocation());
    compare(results, CitedArticle.class, "publisherName", actual.getPublisherName(), expected.getPublisherName());
    comparePageRanges(results, CitedArticle.class, "pages", actual.getPages(), expected.getPages());
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

  private static void compareYear(AssertionCollector results, Integer actualYear, Integer expectedYear) {
    if (actualYear != null && expectedYear != null
        && actualYear.toString().length() == 4 && expectedYear.toString().length() > 4) {
      // On displayYear fields with more than one four-digit substring, legacy behavior is to just concatenate them.
      // New behavior is to just take the first one.
      expectedYear = Integer.valueOf(expectedYear.toString().substring(0, 4));
    }
    compare(results, CitedArticle.class, "year", actualYear, expectedYear);
  }

  private static final Pattern PAGE_RANGE_DELIMITER = Pattern.compile("\\s*-\\s*");

  private static boolean comparePageRanges(AssertionCollector results, Class<?> parentType, String fieldName,
                                           String actualPageRange, String expectedPageRange) {
    if (expectedPageRange != null) {
      // Legacy page range values sometimes have junk whitespace. Remove it before comparing.
      expectedPageRange = PAGE_RANGE_DELIMITER.matcher(expectedPageRange.trim()).replaceAll("-");

      // Legacy page range values sometimes put a hyphen before the last page if the first page was missing.
      if (expectedPageRange.charAt(0) == '-') {
        expectedPageRange = expectedPageRange.substring(1);
      }
    }

    return compare(results, parentType, fieldName, actualPageRange, expectedPageRange);
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
    expected.add(buildExpectedSyndication("PUBMED", article));
    List<Syndication> actual = hibernateTemplate.findByCriteria(
        DetachedCriteria.forClass(Syndication.class)
            .add(Restrictions.eq("doi", article.getDoi()))
            .addOrder(Order.asc("target"))
    );

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

  // Transformation helpers

  private static final Function<Journal, String> JOURNAL_EISSN = new Function<Journal, String>() {
    @Override
    public String apply(Journal input) {
      return input.geteIssn();
    }
  };

  private static final Function<ArticleRelationship, String> RELATIONSHIP_DOI = new Function<ArticleRelationship, String>() {
    @Override
    public String apply(ArticleRelationship input) {
      return input.getOtherArticleDoi();
    }
  };

  private static final Function<ArticleAsset, AssetIdentity> ASSET_IDENTITY = new Function<ArticleAsset, AssetIdentity>() {
    @Override
    public AssetIdentity apply(ArticleAsset input) {
      return AssetIdentity.from(input);
    }
  };

  private static final Function<ArticleAsset, AssetFileIdentity> ASSET_FILE_IDENTITY = new Function<ArticleAsset, AssetFileIdentity>() {
    @Override
    public AssetFileIdentity apply(ArticleAsset input) {
      return AssetFileIdentity.from(input);
    }
  };

  private static final Function<CitedArticle, String> CITATION_KEY = new Function<CitedArticle, String>() {
    @Override
    public String apply(CitedArticle input) {
      return input.getKey();
    }
  };

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
