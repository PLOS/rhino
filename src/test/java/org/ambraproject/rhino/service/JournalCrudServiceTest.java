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

import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.model.Journal;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;

import static org.junit.Assert.assertTrue;

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
  @Ignore
  public void testListJournals() throws IOException {
    Collection<Journal> journals = journalCrudService.getAllJournals();
    assertTrue(journals.size() > 0);
  }

}
