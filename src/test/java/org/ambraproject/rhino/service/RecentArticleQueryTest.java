package org.ambraproject.rhino.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleAsset;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.BaseRhinoTransactionalTest;
import org.ambraproject.rhino.RhinoTestHelper;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.ambraproject.rhino.service.impl.RecentArticleQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RecentArticleQueryTest extends BaseRhinoTransactionalTest {

  @Autowired
  private ArticleCrudService articleCrudService;

  private Article createRecentArticle(String id,
                                      Journal dummyJournal,
                                      Calendar pubDate,
                                      String... types) {
    String doi = DoiBasedIdentity.create(id).getKey();

    Article article = new Article();
    article.setDoi(doi);
    article.setJournals(ImmutableSet.of(dummyJournal));
    article.setDate(pubDate.getTime());

    article.setTitle(id);
    article.setTypes(ImmutableSet.copyOf(types));
    article.setCategories(ImmutableMap.of());

    ArticleAsset xml = new ArticleAsset();
    xml.setDoi(doi);
    xml.setExtension("XML");
    article.setAssets(ImmutableList.of(xml));

    hibernateTemplate.save(article);
    return article;
  }

  private final Calendar baseTimestamp = Calendar.getInstance();

  private Calendar timestamp(int offset) {
    Calendar timestamp = (Calendar) baseTimestamp.clone();
    timestamp.add(Calendar.HOUR, offset);
    return timestamp;
  }

  private final Journal dummyJournal = RhinoTestHelper.createDummyJournal("0");

  private List<?> executeTest(RecentArticleQuery.Builder query) throws IOException {
    query.setJournalKey(dummyJournal.getJournalKey());
    query.setThreshold(timestamp(0));
    return entityGson.fromJson(articleCrudService.listRecent(query.build()).readJson(entityGson), List.class);
  }

  private static void assertDois(List<?> jsonObjects, String... expectedDois) {
    assertEquals(jsonObjects.size(), expectedDois.length);
    for (int i = 0; i < expectedDois.length; i++) {
      Map<?, ?> jsonObject = (Map<?, ?>) jsonObjects.get(i);
      String expectedDoi = expectedDois[i];
      assertEquals(jsonObject.get("doi"), DoiBasedIdentity.create(expectedDoi).getKey());
    }
  }

  /**
   * Assert that the given DOIs appear in the given order, allowing other ones to appear in between.
   */
  private static void assertDoiOrder(List<?> jsonObjects, String... expectedDois) {
    assertTrue(jsonObjects.size() >= expectedDois.length);
    List<String> actualDois = jsonObjects.stream()
        .map(obj -> (String) ((Map<?, ?>) obj).get("doi"))
        .collect(Collectors.toList());
    int lastIndex = -1;
    for (int i = 0; i < expectedDois.length; i++) {
      String expectedDoi = DoiBasedIdentity.create(expectedDois[i]).getKey();
      int index = actualDois.indexOf(expectedDoi);
      assertTrue(index >= 0, "Expected DOI is absent");
      assertTrue(index > lastIndex, "DOI is not in expected order");
      lastIndex = index;
    }
  }

  @Test
  public void testListRecent() throws Exception {
    hibernateTemplate.save(dummyJournal);

    createRecentArticle("veryRecent", dummyJournal, timestamp(2), "t1");
    createRecentArticle("recent", dummyJournal, timestamp(1), "t1", "t2");
    createRecentArticle("stale", dummyJournal, timestamp(-1), "t1", "t3");
    createRecentArticle("otherType", dummyJournal, timestamp(2), "t2", "t3");
    createRecentArticle("issueImageType", dummyJournal, timestamp(2),
        "http://rdf.plos.org/RDF/articleType/Issue%20Image"); // should always be filtered out

    List<?> results;

    results = executeTest(RecentArticleQuery.builder()
        .setArticleTypes(ImmutableList.of("t1")));
    // Expect "t1" type only, in chronological order from most recent to oldest.
    assertDois(results, "veryRecent", "recent");

    results = executeTest(RecentArticleQuery.builder()
        .setMinimum(3)
        .setArticleTypes(ImmutableList.of("t1")));
    // Expect the stale one to be included in order to satisfy the minimum of 3.
    assertDois(results, "veryRecent", "recent", "stale");

    results = executeTest(RecentArticleQuery.builder()
        .setArticleTypes(ImmutableList.of("t1", "*")));
    // "otherType" should be after "veryRecent": it has the same date, but "t1" comes first in type preference
    assertDois(results, "veryRecent", "otherType", "recent");

    results = executeTest(RecentArticleQuery.builder()
        .setArticleTypes(ImmutableList.of("*")));
    // With no type preference order, "otherType" should come before "recent" because it is more recent.
    assertDoiOrder(results, "otherType", "recent");

    results = executeTest(RecentArticleQuery.builder()
        .setMinimum(4)
        .setArticleTypes(ImmutableList.of("t1", "*")));
    // Because it had to exceed the timestamp to get up to the minimum of 4,
    // it ignores article type order and sorts in chronological order.
    assertDoiOrder(results, "otherType", "recent", "stale");

    results = executeTest(RecentArticleQuery.builder()
        .setArticleTypes(ImmutableList.of("t1"))
        .setExcludedArticleTypes(ImmutableList.of("t2")));
    // Get all non-t2 articles within the time window
    assertDois(results, "veryRecent");

    results = executeTest(RecentArticleQuery.builder()
        .setArticleTypes(ImmutableList.of("t1"))
        .setMinimum(2)
        .setExcludedArticleTypes(ImmutableList.of("t2")));
    // Go beyond the time window to get 2 articles, excluding all t2 articles even within the time window
    assertDois(results, "veryRecent", "stale");
  }

}
