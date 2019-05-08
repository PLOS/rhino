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

import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Volume;
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.view.journal.VolumeInputView;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertFalse;

public class VolumeCrudServiceTest extends BaseRhinoTest {

  @Autowired
  private VolumeCrudService volumeCrudService;

  private static final String TEST_JOURNAL_KEY = "journal";

  private Journal createTestJournal() {
    Journal testJournal = new Journal();
    testJournal.setJournalKey(TEST_JOURNAL_KEY);
    hibernateTemplate.save(testJournal);
    return testJournal;
  }

  private Journal getTestJournal() {
    return (Journal) DataAccessUtils.requiredUniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
                .forClass(Journal.class)
                .add(Restrictions.eq("journalKey", TEST_JOURNAL_KEY))
                .setFetchMode("volumes", FetchMode.JOIN)
                .setFetchMode("articleLists", FetchMode.JOIN)
        ));
  }

  @Test
  @Ignore
  public void testCreate() {
    Doi volumeId = Doi.create("10.1371/volume.pmed.v05");
    String displayName = "volumeDisplay";

    String json = String.format(
        "{\"volumeUri\": \"%s\", \"displayName\": \"%s\"}",
        volumeId.getName(), displayName);
    VolumeInputView input = entityGson.fromJson(json, VolumeInputView.class);

    Journal testJournal = createTestJournal();

    volumeCrudService.create(testJournal.getJournalKey(), input);
    testJournal = getTestJournal();

    List<Volume> testJournalVolumes = testJournal.getVolumes();
    assertFalse(testJournalVolumes.isEmpty());
  }

}
