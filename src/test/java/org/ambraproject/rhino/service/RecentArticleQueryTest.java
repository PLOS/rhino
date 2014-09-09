package org.ambraproject.rhino.service;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleAsset;
import org.ambraproject.models.Journal;
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

import static org.testng.Assert.assertEquals;

public class RecentArticleQueryTest extends BaseRhinoTransactionalTest {

  @Autowired
  private ArticleCrudService articleCrudService;

  private Article createRecentArticle(String id,
                                      Journal dummyJournal,
                                      Calendar pubDate,
                                      String... types) {
    Article article = new Article();
    article.setDoi(DoiBasedIdentity.create(id).getKey());
    article.setJournals(ImmutableSet.of(dummyJournal));
    article.setDate(pubDate.getTime());

    article.setTitle(id);
    article.setAssets(ImmutableList.<ArticleAsset>of());
    article.setTypes(ImmutableSet.copyOf(types));

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

  @Test
  public void testListRecent() throws Exception {
    hibernateTemplate.save(dummyJournal);

    createRecentArticle("veryRecent", dummyJournal, timestamp(2), "t1");
    createRecentArticle("recent", dummyJournal, timestamp(1), "t1", "t2");
    createRecentArticle("stale", dummyJournal, timestamp(-1), "t1", "t3");
    createRecentArticle("otherType", dummyJournal, timestamp(3), "t2", "t3");

    List<?> results;

    results = executeTest(RecentArticleQuery.builder()
        .setArticleTypes(ImmutableList.of("t1")));
    assertEquals(results.size(), 2);
    // Expect "t1" type only, in chronological order from most recent to oldest.
    assertEquals(((Map) results.get(0)).get("doi"), "veryRecent");
    assertEquals(((Map) results.get(1)).get("doi"), "recent");

    results = executeTest(RecentArticleQuery.builder()
        .setMinimum(3)
        .setArticleTypes(ImmutableList.of("t1")));
    // Expect the stale one to be included in order to satisfy the minimum of 3.
    assertEquals(results.size(), 3);
    assertEquals(((Map) results.get(0)).get("doi"), "veryRecent");
    assertEquals(((Map) results.get(1)).get("doi"), "recent");
    assertEquals(((Map) results.get(2)).get("doi"), "stale");

    results = executeTest(RecentArticleQuery.builder()
        .setArticleTypes(ImmutableList.of("t1", "*")));
    assertEquals(results.size(), 3);
    // "otherType" should be last, because "t1" comes first in type preference, even though "otherType" is more recent.
    assertEquals(((Map) results.get(0)).get("doi"), "veryRecent");
    assertEquals(((Map) results.get(1)).get("doi"), "recent");
    assertEquals(((Map) results.get(2)).get("doi"), "otherType");

    results = executeTest(RecentArticleQuery.builder()
        .setArticleTypes(ImmutableList.of("*")));
    assertEquals(results.size(), 3);
    // With no type preference order, "otherType" should come first because it is most recent.
    assertEquals(((Map) results.get(0)).get("doi"), "otherType");
    assertEquals(((Map) results.get(1)).get("doi"), "veryRecent");
    assertEquals(((Map) results.get(2)).get("doi"), "recent");

    results = executeTest(RecentArticleQuery.builder()
        .setMinimum(4)
        .setArticleTypes(ImmutableList.of("t1", "*")));
    assertEquals(results.size(), 4);
    // Because it had to exceed the timestamp to get up to the minimum of 4,
    // it ignores article type order and sorts in chronological order.
    assertEquals(((Map) results.get(0)).get("doi"), "otherType");
    assertEquals(((Map) results.get(1)).get("doi"), "veryRecent");
    assertEquals(((Map) results.get(2)).get("doi"), "recent");
    assertEquals(((Map) results.get(3)).get("doi"), "stale");
  }

}
