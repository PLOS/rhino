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

package org.ambraproject.admin.xpath;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.ambraproject.admin.BaseAdminTest;
import org.ambraproject.admin.service.ArticleCrudService;
import org.ambraproject.filestore.FileStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collection;

import static org.testng.Assert.assertEquals;

public class XPathBatchTest extends BaseAdminTest {

  @Autowired
  private ArticleCrudService articleCrudService;
  @Autowired
  private FileStoreService fileStoreService;

  private static final Collection<String> INGEST_XPATH = ImmutableList.copyOf(new String[]{
      "//Article/dublinCore/title",
      "//Article/dublinCore/format/text()",
  });

  @Test
  public void testInferKeysFromIngestQueries() {
    XPathBatch batch = XPathBatch.inferKeysFromIngestQueries(INGEST_XPATH);
    String[] expectedKeys = {"title", "format"};
    assertEquals(batch.asQueryMap().keySet(), ImmutableSet.copyOf(expectedKeys));
    assertEquals(ImmutableSet.copyOf(batch.asQueryMap().values()), ImmutableSet.copyOf(INGEST_XPATH));
  }

  @Test(dataProvider = "sampleArticles")
  public void testXPath(String doi, File fileLocation) throws Exception {
    doi += ".testXPath"; // avoid collisions with canonical sample data
    XPathBatch batch = XPathBatch.inferKeysFromIngestQueries(INGEST_XPATH);
    articleCrudService.create(new TestFile(fileLocation).read(), doi);

    // TODO This would work on transformed XML, not the PMC XML that we have
//    Map<String, String> results = batch.evaluateOnArticle(doi, fileStoreService);
//    assertFalse(results.isEmpty());
  }

}
