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

import com.google.common.collect.ImmutableSet;
import org.ambraproject.rhino.model.Issue;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.rest.RestClientException;
import org.hibernate.criterion.DetachedCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Test for {@link JournalCrudServiceImpl}
 */
public class JournalCrudServiceTest extends BaseRhinoTest {

  private static final String[][] ARTICLE_LIST = new String[][]{
      new String[]{"info:doi/10.1371/journal.pone.3333333", "Title 3"},
      new String[]{"info:doi/10.1371/journal.pone.2222222", "Title 2"},
      new String[]{"info:doi/10.1371/journal.pone.1111111", "Title 1"}
  };

  @Autowired
  private JournalCrudService journalCrudService;

  @Test
  public void testListJournals() throws IOException {
    Map<String, ?> journals = entityGson.fromJson(journalCrudService.listJournals().readJson(entityGson), Map.class);
    assertTrue(journals.size() > 0);
    Map<String, ?> journal = (Map<String, ?>) journals.values().iterator().next();
    assertTrue(journal.keySet().containsAll(ImmutableSet.of("journalKey", "eIssn")));
  }

  @Test
  public void testReadCurrentIssue() throws IOException {
    Journal journal = (Journal) hibernateTemplate.findByCriteria(DetachedCriteria.forClass(Journal.class)).get(0);
    try {
      journalCrudService.readCurrentIssue(journal.getJournalKey()).readJson(entityGson);
      fail("Expected RestClientException");
    } catch (RestClientException e) {
      // expected
    }

    Issue issue = new Issue();
    String testIssueUri = "testIssue";
    issue.setIssueUri(testIssueUri);
    journal.setCurrentIssue(issue);
    hibernateTemplate.update(journal);

    Map<?, ?> currentIssueResult = entityGson.fromJson(
        journalCrudService.readCurrentIssue(journal.getJournalKey()).readJson(entityGson),
        Map.class);
    assertEquals(currentIssueResult.get("issueUri"), testIssueUri);
  }

}