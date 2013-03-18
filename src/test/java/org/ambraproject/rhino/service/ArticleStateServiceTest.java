/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2013 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.service;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import org.ambraproject.models.Article;
import org.ambraproject.models.Journal;
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.content.ArticleState;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.service.impl.ArticleStateServiceImpl;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Tests for {@link org.ambraproject.rhino.service.impl.ArticleStateServiceImpl}
 */
public class ArticleStateServiceTest extends BaseRhinoTest {

  private static final String TEST_DATA_DIR = "src/test/resources/articles/";

  @Autowired
  private ArticleStateService articleStateService;

  @Autowired
  private ArticleCrudService articleCrudService;

  @Autowired
  private Configuration ambraConfiguration;

  @Test
  public void testServiceAutowiring() {
    assertNotNull(articleStateService);
  }

  @BeforeMethod
  public void addJournal() {
    final ImmutableSet<String> testCaseEissns = ImmutableSet.of("1932-6203");

    for (String eissn : testCaseEissns) {
      List<?> existing = hibernateTemplate.findByCriteria(DetachedCriteria
          .forClass(Journal.class)
          .add(Restrictions.eq("eIssn", eissn)));
      if (!existing.isEmpty())
        continue;
      Journal journal = new Journal();
      journal.seteIssn(eissn);
      hibernateTemplate.save(journal);
    }
  }

  @Test
  public void testPublication() throws Exception {
    Article article = articleCrudService.writeArchive(TEST_DATA_DIR + "pone.0056489.zip",
        Optional.<ArticleIdentity>absent(), DoiBasedCrudService.WriteMode.CREATE_ONLY);
    ArticleIdentity articleId = ArticleIdentity.create(article);
    assertEquals(article.getState(), Article.STATE_UNPUBLISHED);

    ArticleState state = articleStateService.read(articleId);
    assertFalse(state.isPublished());
    assertEquals(state.getCrossRefSyndicationState(), ArticleState.SyndicationState.PENDING);
    assertEquals(state.getPmcSyndicationState(), ArticleState.SyndicationState.PENDING);

    state = new ArticleState();
    state.setPublished(true);
    state.setCrossRefSyndicationState(ArticleState.SyndicationState.IN_PROGRESS);
    state.setPmcSyndicationState(ArticleState.SyndicationState.IN_PROGRESS);
    state = articleStateService.write(articleId, state);

    assertTrue(state.isPublished());
    assertEquals(state.getCrossRefSyndicationState(), ArticleState.SyndicationState.IN_PROGRESS);
    assertEquals(state.getPmcSyndicationState(), ArticleState.SyndicationState.IN_PROGRESS);
    ArticleStateServiceImpl impl = (ArticleStateServiceImpl) articleStateService;
    DummyMessageSender dummySender = (DummyMessageSender) impl.messageSender;
    assertEquals(dummySender.messagesSent.size(), 3);

    List<String> solrMessages = dummySender.messagesSent.get("activemq:fake.indexing.queue");
    assertEquals(solrMessages.size(), 1);
    XMLUnit.compareXML(IOUtils.toString(new FileInputStream(
        TEST_DATA_DIR + "pone.0056489_solr_decorated.xml")), solrMessages.get(0));

    String expectedSyndication
        = "<ambraMessage><doi>info:doi/10.1371/journal.pone.0056489</doi><archive>pone.0056489.zip</archive></ambraMessage>";
    List<String> crossRefMessages = dummySender.messagesSent.get("activemq:fake.crossref.queue");
    assertEquals(crossRefMessages.size(), 1);
    XMLUnit.compareXML(expectedSyndication, crossRefMessages.get(0));

    List<String> pmcMessages = dummySender.messagesSent.get("activemq:fake.pmc.queue");
    assertEquals(pmcMessages.size(), 1);
    XMLUnit.compareXML(expectedSyndication, pmcMessages.get(0));
  }
}
