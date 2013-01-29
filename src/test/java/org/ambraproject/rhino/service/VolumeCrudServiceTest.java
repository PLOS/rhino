/*
 * Copyright (c) 2006-2012 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.service;

import org.ambraproject.models.Journal;
import org.ambraproject.models.Volume;
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.identity.DoiBasedIdentity;
import org.hibernate.FetchMode;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.support.DataAccessUtils;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertFalse;

public class VolumeCrudServiceTest extends BaseRhinoTest {

  @Autowired
  private VolumeCrudService volumeCrudService;

  private static final String TEST_JOURNAL_KEY = "journal";

  private Journal getTestJournal() {
    return (Journal) DataAccessUtils.requiredUniqueResult((List<?>)
        hibernateTemplate.findByCriteria(DetachedCriteria
            .forClass(Journal.class)
            .add(Restrictions.eq("journalKey", TEST_JOURNAL_KEY))
            .setFetchMode("volumes", FetchMode.JOIN)
        ));
  }


  @Test
  public void testCreate() {
    DoiBasedIdentity volumeId = DoiBasedIdentity.create("testVolume");
    String displayName = "volumeDisplay";

    Journal testJournal = getTestJournal();
    volumeCrudService.create(volumeId, displayName, testJournal.getJournalKey());

    testJournal = getTestJournal();
    List<Volume> testJournalVolumes = testJournal.getVolumes();
    assertFalse(testJournalVolumes.isEmpty());
  }

}
