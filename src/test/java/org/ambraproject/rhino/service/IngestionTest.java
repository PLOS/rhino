package org.ambraproject.rhino.service;


import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ambraproject.models.AmbraEntity;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.models.ArticlePerson;
import org.ambraproject.models.CitedArticle;
import org.ambraproject.models.CitedArticlePerson;
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.content.PersonName;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.identity.AssetIdentity;
import org.ambraproject.rhino.test.AssertionCollector;
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
import java.util.Collection;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
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

  private static final File DATA_PATH = new File("src/test/resources/data/ingestcase/");
  private static final String JSON_SUFFIX = ".json";
  private static final String XML_SUFFIX = ".xml";

  @Autowired
  private ArticleCrudService articleCrudService;

  /*
   * Use instead of the Spring bean entityGson because it currently has some hackery around journals that is bad here.
   * TODO: Smooth out hackery; use entityGson here.
   */
  private static final Gson GSON = new GsonBuilder().create();

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
    List<Object[]> cases = Lists.newArrayListWithCapacity(jsonFiles.length);
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
      article = GSON.fromJson(input, Article.class);
      threw = false;
    } finally {
      Closeables.close(input, threw);
    }
    return article;
  }

  @Test(dataProvider = "generatedIngestionData")
  public void testIngestion(File jsonFile, File xmlFile) throws Exception {
    final Article expected = readReferenceCase(jsonFile);
    final String caseDoi = expected.getDoi();

    DoiBasedCrudService.WriteResult writeResult =
        articleCrudService.write(new TestFile(xmlFile).read(),
            Optional.<ArticleIdentity>absent(), DoiBasedCrudService.WriteMode.CREATE_ONLY);
    assertEquals(writeResult, DoiBasedCrudService.WriteResult.CREATED, "Service didn't report creating article");

    Article actual = (Article) DataAccessUtils.uniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Article.class)
            .add(Restrictions.eq("doi", caseDoi))));
    assertNotNull(actual, "Failed to create article with expected DOI");

    AssertionCollector results = compareArticle(actual, expected);
    if (results.getFailureCount() > 0) {
      for (AssertionCollector.Failure failure : results.getFailures()) {
        log.error(failure.toString());
      }
      assertEquals(results.getFailureCount(), 0, "Mismatched Article fields");
    }
  }

  private AssertionCollector compareArticle(Article actual, Article expected) {
    AssertionCollector results = new AssertionCollector();

    results.compare(Article.class, "doi", actual.getDoi(), expected.getDoi());
    results.compare(Article.class, "title", actual.getTitle(), expected.getTitle());
    results.compare(Article.class, "eIssn", actual.geteIssn(), expected.geteIssn());
    results.compare(Article.class, "state", actual.getState(), expected.getState());
    results.compare(Article.class, "archiveName", actual.getArchiveName(), expected.getArchiveName());
    results.compare(Article.class, "description", actual.getDescription(), expected.getDescription());
    results.compare(Article.class, "rights", actual.getRights(), expected.getRights());
    results.compare(Article.class, "language", actual.getLanguage(), expected.getLanguage());
    results.compare(Article.class, "format", actual.getFormat(), expected.getFormat());
    results.compare(Article.class, "pages", actual.getPages(), expected.getPages());
    results.compare(Article.class, "eLocationId", actual.geteLocationId(), expected.geteLocationId());
    results.compare(Article.class, "strkImgURI", actual.getStrkImgURI(), expected.getStrkImgURI());
    results.compare(Article.class, "date", actual.getDate(), expected.getDate());
    results.compare(Article.class, "volume", actual.getVolume(), expected.getVolume());
    results.compare(Article.class, "issue", actual.getIssue(), expected.getIssue());
    results.compare(Article.class, "journal", actual.getJournal(), expected.getJournal());
    results.compare(Article.class, "publisherLocation", actual.getPublisherLocation(), expected.getPublisherLocation());
    results.compare(Article.class, "publisherName", actual.getPublisherName(), expected.getPublisherName());
    results.compare(Article.class, "url", actual.getUrl(), expected.getUrl());
    results.compare(Article.class, "collaborativeAuthors", actual.getCollaborativeAuthors(), expected.getCollaborativeAuthors());
    results.compare(Article.class, "types", actual.getTypes(), expected.getTypes());

    comparePersonLists(results, Article.class, "authors", actual.getAuthors(), expected.getAuthors());
    comparePersonLists(results, Article.class, "editors", actual.getEditors(), expected.getEditors());
    compareAssetLists(results, actual.getAssets(), expected.getAssets());
    compareCitationLists(results, actual.getCitedArticles(), expected.getCitedArticles());

    return results;
  }

  private void compareAssetLists(AssertionCollector results,
                                 Collection<ArticleAsset> actualList, Collection<ArticleAsset> expectedList) {
    // Compare assets by their DOI, ignoring order
    ImmutableMap<AssetIdentity, ArticleAsset> actualAssetMap = Maps.uniqueIndex(actualList, GET_ASSET_ID);
    ImmutableSet<AssetIdentity> actualAssetIds = actualAssetMap.keySet();
    ImmutableMap<AssetIdentity, ArticleAsset> expectedAssetMap = Maps.uniqueIndex(expectedList, GET_ASSET_ID);
    ImmutableSet<AssetIdentity> expectedAssetIds = expectedAssetMap.keySet();

    for (AssetIdentity missingDoi : Sets.difference(expectedAssetIds, actualAssetIds)) {
      results.compare(ArticleAsset.class, "doi", null, missingDoi);
    }
    for (AssetIdentity extraDoi : Sets.difference(actualAssetIds, expectedAssetIds)) {
      results.compare(ArticleAsset.class, "doi", extraDoi, null);
    }

    for (AssetIdentity assetDoi : Sets.intersection(actualAssetIds, expectedAssetIds)) {
      compareAssets(results, actualAssetMap.get(assetDoi), actualAssetMap.get(assetDoi));
    }
  }

  private void compareAssets(AssertionCollector results, ArticleAsset actual, ArticleAsset expected) {
    results.compare(ArticleAsset.class, "doi", actual.getDoi(), expected.getDoi());
    results.compare(ArticleAsset.class, "contextElement", actual.getContextElement(), expected.getContextElement());
    results.compare(ArticleAsset.class, "extension", actual.getExtension(), expected.getExtension());
    results.compare(ArticleAsset.class, "contentType", actual.getContentType(), expected.getContentType());
    results.compare(ArticleAsset.class, "title", actual.getTitle(), expected.getTitle());
    results.compare(ArticleAsset.class, "description", actual.getDescription(), expected.getDescription());
    results.compare(ArticleAsset.class, "size", actual.getSize(), expected.getSize());
  }

  private void compareCitationLists(AssertionCollector results,
                                    List<CitedArticle> actualList, List<CitedArticle> expectedList) {
    // Ensure no problems with random access or delayed evaluation
    actualList = ImmutableList.copyOf(actualList);
    expectedList = ImmutableList.copyOf(expectedList);

    // Citations have no guaranteed, database-independent identity, so use list position to match them up.
    // So far Hibernate produces these lists in an order that matches the XML. We depend on this for now.
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
    results.compare(CitedArticle.class, "key", actual.getKey(), expected.getKey());
    results.compare(CitedArticle.class, "year", actual.getYear(), expected.getYear());
    results.compare(CitedArticle.class, "displayYear", actual.getDisplayYear(), expected.getDisplayYear());
    results.compare(CitedArticle.class, "month", actual.getMonth(), expected.getMonth());
    results.compare(CitedArticle.class, "day", actual.getDay(), expected.getDay());
    results.compare(CitedArticle.class, "volumeNumber", actual.getVolumeNumber(), expected.getVolumeNumber());
    results.compare(CitedArticle.class, "volume", actual.getVolume(), expected.getVolume());
    results.compare(CitedArticle.class, "issue", actual.getIssue(), expected.getIssue());
    results.compare(CitedArticle.class, "title", actual.getTitle(), expected.getTitle());
    results.compare(CitedArticle.class, "publisherLocation", actual.getPublisherLocation(), expected.getPublisherLocation());
    results.compare(CitedArticle.class, "publisherName", actual.getPublisherName(), expected.getPublisherName());
    results.compare(CitedArticle.class, "pages", actual.getPages(), expected.getPages());
    results.compare(CitedArticle.class, "eLocationID", actual.geteLocationID(), expected.geteLocationID());
    results.compare(CitedArticle.class, "journal", actual.getJournal(), expected.getJournal());
    results.compare(CitedArticle.class, "note", actual.getNote(), expected.getNote());
    results.compare(CitedArticle.class, "collaborativeAuthors", actual.getCollaborativeAuthors(), expected.getCollaborativeAuthors());
    results.compare(CitedArticle.class, "url", actual.getUrl(), expected.getUrl());
    results.compare(CitedArticle.class, "doi", actual.getDoi(), expected.getDoi());
    results.compare(CitedArticle.class, "summary", actual.getSummary(), expected.getSummary());
    results.compare(CitedArticle.class, "citationType", actual.getCitationType(), expected.getCitationType());

    comparePersonLists(results, CitedArticle.class, "authors", actual.getAuthors(), expected.getAuthors());
    comparePersonLists(results, CitedArticle.class, "editors", actual.getEditors(), expected.getEditors());
  }

  private void comparePersonLists(AssertionCollector results, Class<?> parentType, String fieldName,
                                  List<? extends AmbraEntity> actualList, List<? extends AmbraEntity> expectedList) {
    ImmutableList<PersonName> actualNames = ImmutableList.copyOf(Lists.transform(actualList, AS_PERSON_NAME));
    ImmutableList<PersonName> expectedNames = ImmutableList.copyOf(Lists.transform(expectedList, AS_PERSON_NAME));

    int commonSize = Math.min(actualNames.size(), expectedNames.size());
    for (int i = 0; i < commonSize; i++) {
      results.compare(parentType, fieldName, actualNames.get(i), actualNames.get(i));
    }

    // If the sizes didn't match, report missing/extra citations as errors
    for (int i = commonSize; i < actualList.size(); i++) {
      results.compare(parentType, fieldName, actualNames.get(i), null);
    }
    for (int i = commonSize; i < expectedList.size(); i++) {
      results.compare(parentType, fieldName, null, expectedNames.get(i));
    }
  }

  private static final Function<ArticleAsset, AssetIdentity> GET_ASSET_ID = new Function<ArticleAsset, AssetIdentity>() {
    @Override
    public AssetIdentity apply(ArticleAsset input) {
      String doi = input.getDoi();
      if (doi.startsWith("info:doi/")) {
        doi = doi.substring("info:doi/".length());
      }
      return AssetIdentity.parse(doi + "." + input.getExtension());
    }
  };

  private static final Function<AmbraEntity, PersonName> AS_PERSON_NAME = new Function<AmbraEntity, PersonName>() {
    @Override
    public PersonName apply(AmbraEntity input) {
      // Have to do it this way for the same reason that PersonName exists in the first place -- see PersonName docs
      if (input instanceof ArticlePerson) {
        return PersonName.from((ArticlePerson) input);
      }
      if (input instanceof CitedArticlePerson) {
        return PersonName.from((CitedArticlePerson) input);
      }
      throw new ClassCastException();
    }
  };

}
