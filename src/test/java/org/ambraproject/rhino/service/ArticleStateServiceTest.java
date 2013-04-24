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
import com.google.gson.Gson;
import org.ambraproject.models.Article;
import org.ambraproject.models.Journal;
import org.ambraproject.models.Syndication;
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.content.view.ArticleInputView;
import org.ambraproject.rhino.content.view.ArticleOutputView;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.service.impl.ArticleStateServiceImpl;
import org.ambraproject.service.syndication.SyndicationService;
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
import static org.testng.Assert.assertNotNull;

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
  private SyndicationService syndicationService;

  @Autowired
  private PingbackReadService pingbackReadService;

  @Autowired
  private Configuration ambraConfiguration;

  @Autowired
  private Gson entityGson;

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
    final String crossref = "CROSSREF";
    final String pmc = "PMC";

    Article article = articleCrudService.writeArchive(TEST_DATA_DIR + "pone.0056489.zip",
        Optional.<ArticleIdentity>absent(), DoiBasedCrudService.WriteMode.CREATE_ONLY);
    ArticleIdentity articleId = ArticleIdentity.create(article);
    assertEquals(article.getState(), Article.STATE_UNPUBLISHED);

    ArticleOutputView outputView = ArticleOutputView.create(article, syndicationService, pingbackReadService);
    assertEquals(outputView.getArticle().getState(), Article.STATE_UNPUBLISHED);
    assertEquals(outputView.getSyndication(crossref).getStatus(), Syndication.STATUS_PENDING);
    assertEquals(outputView.getSyndication(pmc).getStatus(), Syndication.STATUS_PENDING);

    String inputJson = ""
        + "{"
        + "  \"state\": \"published\","
        + "  \"syndications\": {"
        + "    \"CROSSREF\": {"
        + "      \"status\": \"IN_PROGRESS\""
        + "    },"
        + "    \"PMC\": {"
        + "      \"status\": \"IN_PROGRESS\""
        + "    }"
        + "  }"
        + "}";
    ArticleInputView inputView = entityGson.fromJson(inputJson, ArticleInputView.class);
    assertEquals(inputView.getPublicationState().get().intValue(), Article.STATE_ACTIVE);
    assertEquals(inputView.getSyndicationUpdate(crossref).getStatus(), Syndication.STATUS_IN_PROGRESS);
    assertEquals(inputView.getSyndicationUpdate(pmc).getStatus(), Syndication.STATUS_IN_PROGRESS);
    article = articleStateService.update(articleId, inputView);

    ArticleOutputView result = ArticleOutputView.create(article, syndicationService, pingbackReadService);
    assertEquals(result.getArticle().getState(), Article.STATE_ACTIVE);
    assertEquals(result.getSyndication(crossref).getStatus(), Syndication.STATUS_IN_PROGRESS);
    assertEquals(result.getSyndication(pmc).getStatus(), Syndication.STATUS_IN_PROGRESS);
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
