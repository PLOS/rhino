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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.ambraproject.models.Annotation;
import org.ambraproject.models.AnnotationType;
import org.ambraproject.models.Article;
import org.ambraproject.models.UserProfile;
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.test.DummyResponseReceiver;
import org.ambraproject.views.AnnotationView;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class AnnotationCrudServiceTest extends BaseRhinoTest {

  @Autowired
  private ArticleCrudService articleCrudService;

  @Autowired
  private AnnotationCrudService annotationCrudService;

  /**
   * Create journals with all eIssn values mentioned in test cases' XML files.
   */
  @BeforeMethod
  public void addJournal() {
    addExpectedJournals();
  }

  @Test
  public void testCommentsAndCorrections() throws Exception {
    String doiStub = SAMPLE_ARTICLES.get(0);
    ArticleIdentity articleId = ArticleIdentity.create(prefixed(doiStub));
    TestFile sampleFile = new TestFile(new File("src/test/resources/articles/" + doiStub + ".xml"));
    String doi = articleId.getIdentifier();
    byte[] sampleData = IOUtils.toByteArray(alterStream(sampleFile.read(), doi, doi));
    TestInputStream input = TestInputStream.of(sampleData);
    Article article = articleCrudService.write(input, Optional.of(articleId),
        DoiBasedCrudService.WriteMode.CREATE_ONLY);

    UserProfile creator = new UserProfile("fake@example.org", "displayName", "password");
    hibernateTemplate.save(creator);
    Annotation correction = new Annotation();
    correction.setCreator(creator);
    correction.setArticleID(article.getID());
    correction.setAnnotationUri("info:doi/10.1371/annotation/test_correction_1");
    correction.setType(AnnotationType.FORMAL_CORRECTION);
    correction.setTitle("Test Correction One");
    correction.setBody("Test Correction One Body");
    hibernateTemplate.save(correction);

    // Reply to the correction.
    Annotation reply = new Annotation();
    reply.setCreator(creator);
    reply.setArticleID(article.getID());
    reply.setAnnotationUri("info:doi/10.1371/reply/test_reply_level_1");
    reply.setParentID(correction.getID());
    reply.setType(AnnotationType.REPLY);
    reply.setTitle("Test Reply Level 1");
    reply.setBody("Test Reply Level 1 Body");
    hibernateTemplate.save(reply);

    // Another first-level reply to the correction.
    Annotation reply2 = new Annotation();
    reply2.setCreator(creator);
    reply2.setArticleID(article.getID());
    reply2.setAnnotationUri("info:doi/10.1371/reply/test_reply_2_level_1");
    reply2.setParentID(correction.getID());
    reply2.setType(AnnotationType.REPLY);
    reply2.setTitle("Test Reply 2 Level 1");
    reply2.setBody("Test Reply 2 Level 1 Body");
    hibernateTemplate.save(reply2);

    // Reply to the first reply.
    Annotation reply3 = new Annotation();
    reply3.setCreator(creator);
    reply3.setArticleID(article.getID());
    reply3.setAnnotationUri("info:doi/10.1371/reply/test_reply_3_level_2");
    reply3.setParentID(reply.getID());
    reply3.setType(AnnotationType.REPLY);
    reply3.setTitle("Test Reply 3 Level 2");
    reply3.setBody("Test Reply 3 Level 2 Body");
    hibernateTemplate.save(reply3);

    Annotation correction2 = new Annotation();
    correction2.setCreator(creator);
    correction2.setArticleID(article.getID());
    correction2.setAnnotationUri("info:doi/10.1371/annotation/test_correction_2");
    correction2.setType(AnnotationType.MINOR_CORRECTION);
    correction2.setTitle("Test Correction Two");
    correction2.setBody("Test Correction Two Body");
    hibernateTemplate.save(correction2);

    Annotation comment = new Annotation();
    comment.setCreator(creator);
    comment.setArticleID(article.getID());
    comment.setAnnotationUri("info:doi/10.1371/annotation/test_comment");
    comment.setType(AnnotationType.COMMENT);
    comment.setTitle("Test Comment");
    comment.setBody("Test Comment Body");
    hibernateTemplate.save(comment);

    DummyResponseReceiver drr = new DummyResponseReceiver();
    annotationCrudService.readCorrections(drr, articleId, MetadataFormat.JSON);
    String json = drr.read();
    assertTrue(json.length() > 0);

    Gson gson = new Gson();
    List<AnnotationView> actualAnnotations = gson.fromJson(json, new TypeToken<List<AnnotationView>>(){}.getType());
    assertEquals(actualAnnotations.size(), 2);
    Map<Long, List<Annotation>> replies = new HashMap<Long, List<Annotation>>();
    replies.put(correction.getID(), Arrays.asList(reply, reply2));
    replies.put(reply.getID(), Arrays.asList(reply3));

    // We can't use vanilla assertEquals because AnnotationView has a property, ID, set to
    // the underlying annotationID.  That property is not in the returned JSON, by design.
    assertAnnotationsEqual(actualAnnotations.get(0),
        new AnnotationView(correction, article.getDoi(), article.getTitle(), replies));
    replies = new HashMap<Long, List<Annotation>>();
    assertAnnotationsEqual(actualAnnotations.get(1),
        new AnnotationView(correction2, article.getDoi(), article.getTitle(), replies));

    // TODO: test comments
  }

  private void assertAnnotationsEqual(AnnotationView actual, AnnotationView expected) {
    assertEquals(actual.getAnnotationUri(), expected.getAnnotationUri());
    assertEquals(actual.getArticleDoi(), expected.getArticleDoi());
    assertEquals(actual.getArticleTitle(), expected.getArticleTitle());
    assertEquals(actual.getParentID(), expected.getParentID());
    assertEquals(actual.getBody(), expected.getBody());
    assertEquals(actual.getCitation(), expected.getCitation());
    assertEquals(actual.getCompetingInterestStatement(), expected.getCompetingInterestStatement());
    assertEquals(actual.getCreatorID(), expected.getCreatorID());
    assertEquals(actual.getCreatorDisplayName(), expected.getCreatorDisplayName());
    assertEquals(actual.getCreatorFormattedName(), expected.getCreatorFormattedName());
    assertEquals(actual.getTitle(), expected.getTitle());
    assertEquals(actual.getType(), expected.getType());
    assertEquals(actual.getTotalNumReplies(), expected.getTotalNumReplies());

    // Don't test this one.  AnnotationView made the dubious choice of setting the last
    // reply date to NOW() if there are no replies.
//    assertEquals(actual.getLastReplyDate(), expected.getLastReplyDate());
    assertEquals(actual.getReplies().length, expected.getReplies().length);
    for (int i = 0; i < actual.getReplies().length; i++) {
      assertAnnotationsEqual(actual.getReplies()[i], expected.getReplies()[i]);
    }
  }
}
