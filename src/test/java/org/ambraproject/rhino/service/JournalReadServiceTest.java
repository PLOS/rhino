/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2014 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.service;

import org.ambraproject.models.Article;
import org.ambraproject.models.ArticleList;
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.util.response.Transceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * Test for {@link JournalReadServiceImpl}
 */
public class JournalReadServiceTest extends BaseRhinoTest {

  private static final String[][] ARTICLE_LIST = new String[][]{
      new String[]{"info:doi/10.1371/journal.pone.3333333", "Title 3"},
      new String[]{"info:doi/10.1371/journal.pone.2222222", "Title 2"},
      new String[]{"info:doi/10.1371/journal.pone.1111111", "Title 1"}
  };

  @Autowired
  private JournalReadService journalReadService;

  @Test
  public void testReadInTheNewsArticles() throws Exception {
    addExpectedJournals();

    // Trigger a bug where JournalReadServiceImpl was returning the DOIs in the order that
    // the articles where created, rather than the order specified in the article list.
    for (String[] arr : ARTICLE_LIST) {
      Article article = new Article();
      article.setDoi(arr[0]);
      article.setTitle(arr[1]);
      hibernateTemplate.save(article);
    }

    ArticleList articleList = new ArticleList();

    // Now reverse the order to save in the article list.
    List<String[]> expected = Arrays.asList(ARTICLE_LIST);
    Collections.reverse(expected);
    List<String> expectedDois = new ArrayList<>(expected.size());
    for (String[] arr : expected) {
      expectedDois.add(arr[0]);
    }
    articleList.setArticleDois(expectedDois);
    articleList.setListCode("testjournal19326203_news");
    hibernateTemplate.save(articleList);

    Transceiver resp = journalReadService.readInTheNewsArticles("TestJournal19326203");
    List actual = entityGson.fromJson(resp.readJson(entityGson), List.class);
    assertEquals(actual.size(), expected.size());
    for (int i = 0; i < actual.size(); i++) {
      Map<String, String> map = (Map<String, String>) actual.get(i);
      assertEquals(map.get("doi"), expected.get(i)[0]);
      assertEquals(map.get("title"), expected.get(i)[1]);
    }
  }
}
