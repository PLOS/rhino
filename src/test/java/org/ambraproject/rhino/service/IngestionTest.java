package org.ambraproject.rhino.service;


import com.google.common.base.CharMatcher;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
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
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.content.PersonName;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetFileIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.test.AssertionCollector;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
  private static final String JSON_SUFFIX = ".json";
  private static final String XML_SUFFIX = ".xml";

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

    // For each JSON file, expect a matching XML file. Ignore XML files without JSON files.
    for (File jsonFile : jsonFiles) {
      String jsonFilePath = jsonFile.getPath();
      String xmlPath = jsonFilePath.substring(0, jsonFilePath.length() - JSON_SUFFIX.length()) + XML_SUFFIX;
      File xmlFile = new File(xmlPath);
      if (!xmlFile.exists()) {
        fail("No XML file to match JSON test case data: " + xmlPath);
      }
      cases.add(new Object[]{jsonFile, xmlFile});
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

    WriteResult writeResult =
        articleCrudService.write(new TestFile(xmlFile).read(),
            Optional.<ArticleIdentity>absent(), DoiBasedCrudService.WriteMode.CREATE_ONLY);
    assertEquals(writeResult.getAction(), WriteResult.Action.CREATED, "Service didn't report creating article");

    Article actual = (Article) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Article.class)
            .setFetchMode("journals", FetchMode.JOIN)
            .add(Restrictions.eq("doi", caseDoi))));
    assertNotNull(actual, "Failed to create article with expected DOI");

    AssertionCollector results = compareArticle(actual, expected);
    log.info("{} successes", results.getSuccessCount());
    Collection<AssertionCollector.Failure> failures = results.getFailures();
    for (AssertionCollector.Failure failure : failures) {
      log.error(failure.toString());
    }
    assertEquals(failures.size(), 0, "Mismatched Article fields");
  }

  private AssertionCollector compareArticle(Article actual, Article expected) {
    AssertionCollector results = new AssertionCollector();
    compareArticleFields(results, actual, expected);
    comparePersonLists(results, Article.class, "authors", actual.getAuthors(), expected.getAuthors());
    comparePersonLists(results, Article.class, "editors", actual.getEditors(), expected.getEditors());
    compareCategorySets(results, actual.getCategories(), expected.getCategories());
    compareJournalSets(results, actual.getJournals(), expected.getJournals());
    compareRelationshipLists(results, actual.getRelatedArticles(), expected.getRelatedArticles());
    compareAssetLists(results, actual.getAssets(), expected.getAssets());
    compareCitations(results, actual.getCitedArticles(), expected.getCitedArticles());
    return results;
  }

  /**
   * Compare two text values in which substrings of whitespace are interchangeable. The typical case is HMTL text or XML
   * text that will be transformed into HTML.
   */
  private static boolean compareMarkupText(AssertionCollector results,
                                           Class<?> objectType, String fieldName,
                                           CharSequence actual, CharSequence expected) {
    return results.compare(objectType, fieldName, collapseWhitespace(actual), collapseWhitespace(expected));
  }

  private static String collapseWhitespace(CharSequence text) {
    return (text == null) ? null : CharMatcher.WHITESPACE.collapseFrom(text, ' ');
  }

  /**
   * Compare simple (non-associative) fields.
   *
   * @param results
   * @param actual
   * @param expected
   */
  private void compareArticleFields(AssertionCollector results, Article actual, Article expected) {
    results.compare(Article.class, "doi", actual.getDoi(), expected.getDoi());
    compareMarkupText(results, Article.class, "title", actual.getTitle(), expected.getTitle());
    results.compare(Article.class, "eIssn", actual.geteIssn(), expected.geteIssn());
    results.compare(Article.class, "state", actual.getState(), expected.getState());
    compareMarkupText(results, Article.class, "description", actual.getDescription(), expected.getDescription());
    compareMarkupText(results, Article.class, "rights", actual.getRights(), expected.getRights());
    results.compare(Article.class, "language", actual.getLanguage(), expected.getLanguage());
    results.compare(Article.class, "format", actual.getFormat(), expected.getFormat());
    results.compare(Article.class, "pages", actual.getPages(), expected.getPages());
    results.compare(Article.class, "eLocationId", actual.geteLocationId(), expected.geteLocationId());

    // TODO: Test archiveName field when Rhino has a design for if and how to store the article as a .zip archive
    //    results.compare(Article.class, "archiveName", actual.getArchiveName(), expected.getArchiveName());

    // Skip striking image field as long as it's not set as part of ingesting NLM DTD
    //    results.compare(Article.class, "strkImgURI",
    //        Strings.nullToEmpty(actual.getStrkImgURI()),
    //        Strings.nullToEmpty(expected.getStrkImgURI()));

    // actual.getDate() returns a java.sql.Date since it's coming from hibernate.  We have
    // to convert that to a java.util.Date (which GSON returns) for the comparison.
    results.compare(Article.class, "date", new Date(actual.getDate().getTime()),
        expected.getDate());
    results.compare(Article.class, "volume", actual.getVolume(), expected.getVolume());
    results.compare(Article.class, "issue", actual.getIssue(), expected.getIssue());
    results.compare(Article.class, "journal", actual.getJournal(), expected.getJournal());
    results.compare(Article.class, "publisherLocation", actual.getPublisherLocation(), expected.getPublisherLocation());
    results.compare(Article.class, "publisherName", actual.getPublisherName(), expected.getPublisherName());
    results.compare(Article.class, "url", actual.getUrl(), expected.getUrl());
    results.compare(Article.class, "collaborativeAuthors", actual.getCollaborativeAuthors(), expected.getCollaborativeAuthors());
    results.compare(Article.class, "types", actual.getTypes(), expected.getTypes());
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
      results.compare(Journal.class, "eIssn", null, missing);
    }
    for (String extra : Sets.difference(actualEissns, expectedEissns)) {
      results.compare(Journal.class, "eIssn", extra, null);
    }
    for (String eissn : Sets.intersection(actualEissns, expectedEissns)) {
      results.compare(Journal.class, "eIssn", eissn, eissn);
    }
  }

  private void compareRelationshipLists(AssertionCollector results, List<ArticleRelationship> actual, List<ArticleRelationship> expected) {
    /*
     * Ignore this field. No known cases where it would be defined by article XML.
     */
  }

  private void compareAssetLists(AssertionCollector results,
                                 Collection<ArticleAsset> actualList, Collection<ArticleAsset> expectedList) {
    // Compare assets by their DOI, ignoring order
    Map<AssetIdentity, ArticleAsset> actualAssetMap = mapUninitAssetsById(actualList);
    Set<AssetIdentity> actualAssetIds = actualAssetMap.keySet();
    Multimap<AssetIdentity, ArticleAsset> expectedAssetMap = mapAssetFilesByAssetId(expectedList);
    Set<AssetIdentity> expectedAssetIds = expectedAssetMap.keySet();

    for (AssetIdentity missingDoi : Sets.difference(expectedAssetIds, actualAssetIds)) {
      results.compare(ArticleAsset.class, "doi", null, missingDoi);
    }
    for (AssetIdentity extraDoi : Sets.difference(actualAssetIds, expectedAssetIds)) {
      results.compare(ArticleAsset.class, "doi", extraDoi, null);
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
        compareAssetFields(results, actualAsset, expectedAsset);
      }
    }
  }

  /**
   * Compare only those fields that can be gotten from article XML.
   *
   * @param results  the object into which to insert results
   * @param actual   an actual asset with no information specific to an uploaded file
   * @param expected an expected asset with an associated file
   */
  private void compareAssetFields(AssertionCollector results, ArticleAsset actual, ArticleAsset expected) {
    assertEquals(actual.getDoi(), expected.getDoi()); // should be true as a method precondition

    results.compare(ArticleAsset.class, "contextElement", actual.getContextElement(), expected.getContextElement());
    compareMarkupText(results, ArticleAsset.class, "title", actual.getTitle(), expected.getTitle());
    compareMarkupText(results, ArticleAsset.class, "description", actual.getDescription(), expected.getDescription());

    // These are skipped because they would require a file upload before we could know them.
    //    results.compare(ArticleAsset.class, "extension", actual.getExtension(), expected.getExtension());
    //    results.compare(ArticleAsset.class, "contentType", actual.getContentType(), expected.getContentType());
    //    results.compare(ArticleAsset.class, "size", actual.getSize(), expected.getSize());
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

  private void compareCitations(AssertionCollector results,
                                List<CitedArticle> actualList, List<CitedArticle> expectedList) {
    for (CitedArticle expectedCitation : expectedList) {
      if (expectedCitation.getKey() == null) {
        // At least one expected case has null keys. Fall back on comparing by order.
        compareCitationLists(results, actualList, expectedList);
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
      results.compare(Article.class, "citedArticles", actualMap.get(extra), null);
    }
    for (String missing : Sets.difference(expectedKeys, actualKeys)) {
      results.compare(Article.class, "citedArticles", null, expectedMap.get(missing));
    }
  }

  private void compareCitationLists(AssertionCollector results,
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
      results.compare(Article.class, "citedArticles", actualList.get(i), null);
    }
    for (int i = commonSize; i < expectedList.size(); i++) {
      results.compare(Article.class, "citedArticles", null, expectedList.get(i));
    }
  }

  private void compareCitations(AssertionCollector results, CitedArticle actual, CitedArticle expected) {
    if (isEmptyCitation(expected)) {
      return; // Apparently these occur because of an Admin bug. Assume the actual data is correct.
    }

    results.compare(CitedArticle.class, "key", actual.getKey(), expected.getKey());
    results.compare(CitedArticle.class, "year", actual.getYear(), expected.getYear());
    results.compare(CitedArticle.class, "displayYear", actual.getDisplayYear(), expected.getDisplayYear());
    results.compare(CitedArticle.class, "month", actual.getMonth(), expected.getMonth());
    results.compare(CitedArticle.class, "day", actual.getDay(), expected.getDay());
    results.compare(CitedArticle.class, "volumeNumber", actual.getVolumeNumber(), expected.getVolumeNumber());
    results.compare(CitedArticle.class, "volume", actual.getVolume(), expected.getVolume());
    results.compare(CitedArticle.class, "issue", actual.getIssue(), expected.getIssue());
    compareMarkupText(results, CitedArticle.class, "title", actual.getTitle(), expected.getTitle());
    results.compare(CitedArticle.class, "publisherLocation", actual.getPublisherLocation(), expected.getPublisherLocation());
    results.compare(CitedArticle.class, "publisherName", actual.getPublisherName(), expected.getPublisherName());
    results.compare(CitedArticle.class, "pages", actual.getPages(), expected.getPages());
    results.compare(CitedArticle.class, "eLocationID", actual.geteLocationID(), expected.geteLocationID());
    results.compare(CitedArticle.class, "journal", actual.getJournal(), expected.getJournal());
    compareMarkupText(results, CitedArticle.class, "note", actual.getNote(), expected.getNote());
    results.compare(CitedArticle.class, "collaborativeAuthors", actual.getCollaborativeAuthors(), expected.getCollaborativeAuthors());
    results.compare(CitedArticle.class, "url", actual.getUrl(), expected.getUrl());
    results.compare(CitedArticle.class, "doi", actual.getDoi(), expected.getDoi());
    results.compare(CitedArticle.class, "summary", actual.getSummary(), expected.getSummary());
    results.compare(CitedArticle.class, "citationType", actual.getCitationType(), expected.getCitationType());

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

      results.compare(field, "fullName", actualName.getFullName(), expectedName.getFullName());
      results.compare(field, "givenNames", actualName.getGivenNames(), expectedName.getGivenNames());
      results.compare(field, "surname", actualName.getSurname(), expectedName.getSurname());
      results.compare(field, "suffix", actualName.getSuffix(), expectedName.getSuffix());
    }

    // If the sizes didn't match, report missing/extra elements as errors
    for (int i = commonSize; i < actualList.size(); i++) {
      results.compare(parentType, fieldName, actualNames.get(i), null);
    }
    for (int i = commonSize; i < expectedList.size(); i++) {
      results.compare(parentType, fieldName, null, expectedNames.get(i));
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
