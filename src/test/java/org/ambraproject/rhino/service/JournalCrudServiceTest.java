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

import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.model.Journal;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;

import static org.testng.Assert.assertTrue;

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

  @Test(enabled = false)
  public void testListJournals() throws IOException {
    Collection<Journal> journals = journalCrudService.getAllJournals();
    assertTrue(journals.size() > 0);
  }

}
