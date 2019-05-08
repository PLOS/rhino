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

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.RhinoTestHelper;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleRelationship;
import org.ambraproject.rhino.model.Category;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.article.ArticleMetadata;
import org.ambraproject.rhino.model.article.AssetMetadata;
import org.ambraproject.rhino.model.article.NlmPerson;
import org.ambraproject.rhino.model.article.RelatedArticleLink;
import org.ambraproject.rhino.test.AssertionCollector;
import org.ambraproject.rhino.util.StringReplacer;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import com.tngtech.java.junit.dataprovider.DataProvider;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

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
@Ignore
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
    return (dir, name) -> name.endsWith(suffix);
  }

  /**
   * Provide test cases for ingestion tests.
   *
   * @param assertionSuffix the file name suffix for files that assert expected behavior
   * @param testInputSuffix the file name suffix for files to use as test input
   * @param testDataPath    the path to the directory containing both above kinds of file
   * @return a list containing arrays ready to pass as @Test parameters
   */
  private static List<Object[]> provideIngestionCases(String assertionSuffix, String testInputSuffix, File testDataPath) {
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
  public static Object[][] generatedIngestionData() {
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
  public static Object[][] generatedZipIngestionData() {
    return provideIngestionCases(JSON_SUFFIX, ZIP_SUFFIX, ZIP_DATA_PATH).toArray(new Object[0][]);
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

//  @Test(dataProvider = "generatedIngestionData")
//  @Ignore
//  public void testIngestion(File jsonFile, File xmlFile) throws Exception {
//    final Article expected = RhinoTestHelper.readReferenceCase(jsonFile);
//    createTestJournal(expected.geteIssn());
//    final String caseDoi = expected.getDoi();
//
//    RhinoTestHelper.TestInputStream testInputStream = new RhinoTestHelper.TestFile(xmlFile).read();
//    InputStream mockIngestible = IngestibleUtil.buildMockIngestible(testInputStream, expected.getAssets());
//    Archive ingestible = Archive.readZipFileIntoMemory(xmlFile.getName() + ".zip", mockIngestible);
//    Article actual = articleCrudService.writeArchive(ingestible,
//        Optional.empty(), DoiBasedCrudService.WriteMode.CREATE_ONLY, OptionalInt.empty());
//    assertTrue(actual.getID() > 0, "Article doesn't have a database ID");
//    assertTrue(actual.getCreated() != null, "Article doesn't have a creation date");
//
//    // Reload the article directly from hibernate, just to be sure.
//    actual = (Article) DataAccessUtils.uniqueResult((List<?>)
//        hibernateTemplate.findByCriteria(DetachedCriteria
//            .forClass(Article.class)
//            .setFetchMode("journals", FetchMode.JOIN)
//            .setFetchMode("journals.volumes", FetchMode.JOIN)
//            .setFetchMode("journals.articleList", FetchMode.JOIN)
//            .add(Restrictions.eq("doi", caseDoi))));
//    assertNotNull(actual, "Failed to create article with expected DOI");
//
//    AssertionCollector results = compareArticle(actual, expected);
//    log.info("{} successes", results.getSuccessCount());
//    Collection<AssertionCollector.Failure> failures = results.getFailures();
//    for (AssertionCollector.Failure failure : failures) {
//      log.error(failure.toString());
//    }
//    assertEquals(failures.size(), 0, "Mismatched Article fields for " + expected.getDoi());
//    testReadMetadata(actual);
//  }

//  private void testReadMetadata(Article article) throws IOException {
//    // Mostly we want to test that this method call doesn't crash or hang
//    Transceiver response = articleCrudService.serveMetadata(article, true);
//
//    assertFalse(StringUtils.isBlank(response.readJson(entityGson)));
//  }

//  @Test(dataProvider = "generatedZipIngestionData")
//  @Ignore
//  public void testZipIngestion(File jsonFile, File zipFile) throws Exception {
//    final Article expected = RhinoTestHelper.readReferenceCase(jsonFile);
//    createTestJournal(expected.geteIssn());
//    Article actual = articleCrudService.writeArchive(Archive.readZipFileIntoMemory(zipFile),
//        Optional.empty(), DoiBasedCrudService.WriteMode.CREATE_ONLY, OptionalInt.empty());
//    assertTrue(actual.getID() > 0, "Article doesn't have a database ID");
//    assertTrue(actual.getCreated() != null, "Article doesn't have a creation date");
//
//    // Reload the article directly from hibernate, just to be sure.
//    actual = (Article) DataAccessUtils.uniqueResult((List<?>)
//        hibernateTemplate.findByCriteria(DetachedCriteria
//            .forClass(Article.class)
//            .setFetchMode("journals", FetchMode.JOIN)
//            .add(Restrictions.eq("doi", expected.getDoi()))));
//    assertNotNull(actual, "Failed to create article with expected DOI");
//    AssertionCollector results = compareArticle(actual, expected);
//    log.info("{} successes", results.getSuccessCount());
//
//    // Do some additional comparisons that only make sense for an article ingested from an archive.
//    compareArchiveFields(results, actual, expected);
//    Collection<AssertionCollector.Failure> failures = results.getFailures();
//    for (AssertionCollector.Failure failure : failures) {
//      log.error(failure.toString());
//    }
//    assertEquals(failures.size(), 0, "Mismatched Article fields for " + expected.getDoi());
//  }

//  @Test
//  @Ignore
//  public void testReingestion() throws Exception {
//    createTestJournal("1932-6203");
//    long start = System.currentTimeMillis();
//    Archive zipPath = Archive.readZipFileIntoMemory(new File(
//        ZIP_DATA_PATH.getCanonicalPath() + File.separator + "pone.0056489.zip"));
//    Article first = articleCrudService.writeArchive(zipPath,
//        Optional.empty(), DoiBasedCrudService.WriteMode.CREATE_ONLY, OptionalInt.empty());
//    assertTrue(first.getID() > 0, "Article doesn't have a database ID");
//    assertTrue(first.getCreated().getTime() >= start);
//
//    try {
//      Article second = articleCrudService.writeArchive(zipPath,
//          Optional.empty(), DoiBasedCrudService.WriteMode.CREATE_ONLY, OptionalInt.empty());
//      fail("Article creation succeeded for second ingestion in CREATE_ONLY mode");
//    } catch (RestClientException expected) {
//      assertEquals(expected.getResponseStatus(), HttpStatus.METHOD_NOT_ALLOWED);
//    }
//
//    Article second = articleCrudService.writeArchive(zipPath,
//        Optional.empty(), DoiBasedCrudService.WriteMode.WRITE_ANY, OptionalInt.empty());
//
//    // TODO: figure out how to detect that second was re-ingested.  Don't want to
//    // use modification time since the test might run in less than one clock tick.
//    assertTrue(first.getID() > 0, "Article doesn't have a database ID");
//  }

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

  private AssertionCollector compareArticle(ArticleMetadata actual, ArticleMetadata expected) {
    AssertionCollector results = new AssertionCollector();
    compareArticleFields(results, actual, expected);
//    comparePersonLists(results, Article.class, "authors", actual.getAuthors(), expected.getAuthors());
    comparePersonLists(results, Article.class, "editors", actual.getEditors(), expected.getEditors());
    compareRelationshipLists(results, actual.getRelatedArticles(), expected.getRelatedArticles());
    compareAssetsWithExpectedFiles(results, actual.getAssets(), expected.getAssets());

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
  private void compareArticleFields(AssertionCollector results, ArticleMetadata actual, ArticleMetadata expected) {
    compare(results, Article.class, "doi", actual.getDoi(), expected.getDoi());
    compareMarkupText(results, Article.class, "title", actual.getTitle(), expected.getTitle());
    compare(results, Article.class, "eIssn", actual.geteIssn(), expected.geteIssn());
    compareMarkupText(results, Article.class, "description", actual.getDescription(), expected.getDescription());
    compareRights(results, actual.getRights(), expected.getRights());
    compare(results, Article.class, "language", actual.getLanguage(), expected.getLanguage());
    compare(results, Article.class, "format", actual.getFormat(), expected.getFormat());
    compare(results, Article.class, "pages", actual.getPageCount(), expected.getPageCount());
    compare(results, Article.class, "eLocationId", actual.geteLocationId(), expected.geteLocationId());
    compare(results, Article.class, "publicationDate", actual.getPublicationDate(), expected.getPublicationDate());
    compare(results, Article.class, "volume", actual.getVolume(), expected.getVolume());
    compare(results, Article.class, "issue", actual.getIssue(), expected.getIssue());
    compare(results, Article.class, "publisherLocation", actual.getPublisherLocation(), expected.getPublisherLocation());
    compare(results, Article.class, "publisherName", actual.getPublisherName(), expected.getPublisherName());
    compare(results, Article.class, "url", actual.getUrl(), expected.getUrl());
    compare(results, Article.class, "types", actual.getArticleType(), expected.getArticleType());
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

  private void compareRelationshipLists(AssertionCollector results, List<RelatedArticleLink> actual, List<RelatedArticleLink> expected) {
    Map<ArticleIdentifier, RelatedArticleLink> actualMap = Maps.uniqueIndex(actual, RelatedArticleLink::getArticleId);
    Set<ArticleIdentifier> actualDois = actualMap.keySet();
    Map<ArticleIdentifier, RelatedArticleLink> expectedMap = Maps.uniqueIndex(expected, RelatedArticleLink::getArticleId);
    Set<ArticleIdentifier> expectedDois = expectedMap.keySet();

    for (ArticleIdentifier missingDoi : Sets.difference(expectedDois, actualDois)) {
      compare(results, ArticleRelationship.class, "otherArticleDoi", null, missingDoi);
    }
    for (ArticleIdentifier extraDoi : Sets.difference(actualDois, expectedDois)) {
      compare(results, ArticleRelationship.class, "otherArticleDoi", extraDoi, null);
    }

    for (ArticleIdentifier doi : Sets.intersection(actualDois, expectedDois)) {
      compare(results, ArticleRelationship.class, "otherArticleDoi", doi, doi);
      compare(results, ArticleRelationship.class, "type", actualMap.get(doi).getType(), expectedMap.get(doi).getType());
    }
  }

  private void compareAssetsWithExpectedFiles(AssertionCollector results,
                                              Collection<AssetMetadata> actual, Collection<AssetMetadata> expected) {
    Map<String, AssetMetadata> actualMap = Maps.uniqueIndex(actual, AssetMetadata::getDoi);
    Set<String> actualKeys = actualMap.keySet();
    Map<String, AssetMetadata> expectedMap = Maps.uniqueIndex(expected, AssetMetadata::getDoi);
    Set<String> expectedKeys = expectedMap.keySet();

    for (String missing : Sets.difference(expectedKeys, actualKeys)) {
      compare(results, AssetMetadata.class, "doi", null, missing);
    }
    for (String extra : Sets.difference(actualKeys, expectedKeys)) {
      compare(results, AssetMetadata.class, "doi", extra, null);
    }
    for (String key : Sets.intersection(actualKeys, expectedKeys)) {
      compareAssetFields(results, actualMap.get(key), expectedMap.get(key));
    }
  }

  /**
   * Compare only those fields that can be gotten from article XML.
   *
   * @param results  the object into which to insert results
   * @param actual   an actual asset with no information specific to an uploaded file
   * @param expected an expected asset with an associated file
   */
  private void compareAssetFields(AssertionCollector results, AssetMetadata actual, AssetMetadata expected) {
    assertEquals(actual.getDoi(), expected.getDoi()); // should be true as a method precondition

    compareMarkupText(results, AssetMetadata.class, "title(" + actual.getDoi() + ")", actual.getTitle(), expected.getTitle());
    compareMarkupText(results, AssetMetadata.class, "description(" + actual.getDoi() + ")", actual.getDescription(), expected.getDescription());
  }

  private void comparePersonLists(AssertionCollector results, Class<?> parentType, String fieldName,
                                  List<NlmPerson> actualNames, List<NlmPerson> expectedNames) {
    final String field = parentType.getSimpleName() + "." + fieldName;

    int commonSize = Math.min(actualNames.size(), expectedNames.size());
    for (int i = 0; i < commonSize; i++) {
      NlmPerson actualName = actualNames.get(i);
      NlmPerson expectedName = expectedNames.get(i);

      compare(results, field, "fullName", actualName.getFullName(), expectedName.getFullName());
      compare(results, field, "givenNames", actualName.getGivenNames(), expectedName.getGivenNames());
      compare(results, field, "surname", actualName.getSurname(), expectedName.getSurname());
      compare(results, field, "suffix", actualName.getSuffix(), expectedName.getSuffix());
    }

    // If the sizes didn't match, report missing/extra elements as errors
    for (int i = commonSize; i < actualNames.size(); i++) {
      compare(results, parentType, fieldName, actualNames.get(i), null);
    }
    for (int i = commonSize; i < expectedNames.size(); i++) {
      compare(results, parentType, fieldName, null, expectedNames.get(i));
    }
  }

}
