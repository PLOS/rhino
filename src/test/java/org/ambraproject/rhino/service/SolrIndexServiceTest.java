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

import com.google.common.collect.ImmutableSet;
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.RhinoTestHelper;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.service.impl.SolrIndexServiceImpl;
import org.ambraproject.rhino.util.Archive;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Tests for {@link SolrIndexServiceImpl}
 */
public class SolrIndexServiceTest extends BaseRhinoTest {

  private static final String TEST_DATA_DIR = "src/test/resources/articles/";

  @Autowired
  private SolrIndexService solrIndexService;

  @Test
  public void testServiceAutowiring() {
    assertNotNull(solrIndexService);
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
      Journal journal = RhinoTestHelper.createDummyJournal(eissn);
      hibernateTemplate.save(journal);
    }
  }

  @Test(enabled = false)
  public void testPublication() throws Exception {
    Archive archive = Archive.readZipFileIntoMemory(new File(TEST_DATA_DIR + "pone.0056489.zip"));
//    Article article = articleCrudService.writeArchive(archive,
//        Optional.empty(), DoiBasedCrudService.WriteMode.CREATE_ONLY, OptionalInt.empty());
    Article article = new Article();
    ArticleIdentifier articleId = ArticleIdentifier.create(article.getDoi());

    SolrIndexServiceImpl impl = (SolrIndexServiceImpl) solrIndexService;
    DummyMessageSender dummySender = (DummyMessageSender) impl.messageSender;
    assertEquals(dummySender.messagesSent.size(), 5);

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

    List<String> pubmedMessages = dummySender.messagesSent.get("activemq:fake.pubmed.queue");
    assertEquals(pubmedMessages.size(), 1);
    XMLUnit.compareXML(expectedSyndication, pubmedMessages.get(0));

    solrIndexService.removeSolrIndex(articleId);
    assertEquals(dummySender.messagesSent.size(), 6);
    List<String> deletionMessages = dummySender.messagesSent.get("activemq:fake.delete.queue");
    assertEquals(deletionMessages.size(), 1);
    assertEquals(deletionMessages.get(0), article.getDoi());
  }
}
