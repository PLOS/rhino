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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.ambraproject.models.Annotation;
import org.ambraproject.models.AnnotationType;
import org.ambraproject.models.Article;
import org.ambraproject.models.UserProfile;
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.IngestibleUtil;
import org.ambraproject.rhino.RhinoTestHelper;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.views.AnnotationView;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class AnnotationCrudServiceTest extends BaseRhinoTest {

  @Autowired
  private ArticleCrudService articleCrudService;

  @Autowired
  private AnnotationCrudService annotationCrudService;

  @Autowired
  protected Gson entityGson;

  /**
   * Create journals with all eIssn values mentioned in test cases' XML files.
   */
  @BeforeMethod
  public void addJournal() {
    addExpectedJournals();
  }

  @Test
  public void testComments() throws Exception {
    String doiStub = RhinoTestHelper.SAMPLE_ARTICLES.get(0);
    ArticleIdentity articleId = ArticleIdentity.create(RhinoTestHelper.prefixed(doiStub));
    RhinoTestHelper.TestFile sampleFile = new RhinoTestHelper.TestFile(new File(
        "src/test/resources/articles/" + doiStub + ".xml"));
    String doi = articleId.getIdentifier();
    byte[] sampleData = IOUtils.toByteArray(RhinoTestHelper.alterStream(sampleFile.read(), doi, doi));
    RhinoTestHelper.TestInputStream input = RhinoTestHelper.TestInputStream.of(sampleData);
    Archive ingestible = Archive.readZipFileIntoMemory(articleId.getLastToken() + ".zip",
        IngestibleUtil.buildMockIngestible(input));
    Article article = writeToLegacy(articleCrudService, ingestible);

    UserProfile creator = new UserProfile("fake@example.org", "displayName", "password");
    hibernateTemplate.save(creator);
    Annotation comment1 = new Annotation();
    comment1.setCreator(creator);
    comment1.setArticleID(article.getID());
    comment1.setAnnotationUri("info:doi/10.1371/annotation/test_comment_1");
    comment1.setType(AnnotationType.COMMENT);
    comment1.setTitle("Test Comment One");
    comment1.setBody("Test Comment One Body");
    hibernateTemplate.save(comment1);
    Date commentCreated = new Date();

    // Reply to the comment.
    Annotation reply = new Annotation();
    reply.setCreator(creator);
    reply.setArticleID(article.getID());
    reply.setAnnotationUri("info:doi/10.1371/reply/test_reply_level_1");
    reply.setParentID(comment1.getID());
    reply.setType(AnnotationType.REPLY);
    reply.setTitle("Test Reply Level 1");
    reply.setBody("Test Reply Level 1 Body");
    hibernateTemplate.save(reply);

    // Another first-level reply to the comment.
    Annotation reply2 = new Annotation();
    reply2.setCreator(creator);
    reply2.setArticleID(article.getID());
    reply2.setAnnotationUri("info:doi/10.1371/reply/test_reply_2_level_1");
    reply2.setParentID(comment1.getID());
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

    Annotation comment2 = new Annotation();
    comment2.setCreator(creator);
    comment2.setArticleID(article.getID());
    comment2.setAnnotationUri("info:doi/10.1371/annotation/test_comment_2");
    comment2.setType(AnnotationType.COMMENT);
    comment2.setTitle("Test Comment Two");
    comment2.setBody("Test Comment Two Body");
    hibernateTemplate.save(comment2);

    Annotation comment3 = new Annotation();
    comment3.setCreator(creator);
    comment3.setArticleID(article.getID());
    comment3.setAnnotationUri("info:doi/10.1371/annotation/test_comment_3");
    comment3.setType(AnnotationType.COMMENT);
    comment3.setTitle("Test Comment");
    comment3.setBody("Test Comment Body");
    hibernateTemplate.save(comment3);

    String json = annotationCrudService.readComments(articleId).readJson(entityGson);
    assertTrue(json.length() > 0);

    // Confirm that date strings in the JSON are formatted as ISO8601 ("2012-04-23T18:25:43.511Z").
    // We have to do this at a lower level since AnnotationView exposes the created field as
    // a Java Date instead of a String.
    Gson gson = new Gson();
    List commentsList = gson.fromJson(json, List.class);
    Map commentMap = (Map) commentsList.get(0);
    String createdStr = (String) commentMap.get("created");

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'");
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

    // TODO: this might fail near midnight, if we're unlucky and the hibernate
    // entity gets saved just before midnight, and this is executed just after.
    assertTrue(createdStr.startsWith(dateFormat.format(commentCreated)), createdStr);

    // Now deserialize to AnnotationView to do more comparisons.
    List<AnnotationView> actualAnnotations = entityGson.fromJson(json,
        new TypeToken<List<AnnotationView>>() {
        }.getType()
    );
    assertEquals(actualAnnotations.size(), 3);
    Map<Long, List<Annotation>> replies = new HashMap<Long, List<Annotation>>();
    replies.put(comment1.getID(), Arrays.asList(reply, reply2));
    replies.put(reply.getID(), Arrays.asList(reply3));

    // We can't use vanilla assertEquals because AnnotationView has a property, ID, set to
    // the underlying annotationID.  That property is not in the returned JSON, by design.
    assertAnnotationsEqual(actualAnnotations.get(0),
        new AnnotationView(comment1, article.getDoi(), article.getTitle(), replies));
    replies = new HashMap<Long, List<Annotation>>();
    assertAnnotationsEqual(actualAnnotations.get(1),
        new AnnotationView(comment2, article.getDoi(), article.getTitle(), replies));

    // Comment with no replies.
    json = annotationCrudService.readComments(articleId).readJson(entityGson);
    List<AnnotationView> actualComments = entityGson.fromJson(json, new TypeToken<List<AnnotationView>>() {
    }.getType());
    assertEquals(actualComments.size(), 3);
    replies = new HashMap<Long, List<Annotation>>();
    assertAnnotationsEqual(actualComments.get(2),
        new AnnotationView(comment3, article.getDoi(), article.getTitle(), replies));
  }

  private void assertAnnotationsEqual(AnnotationView actual, AnnotationView expected) {
    assertEquals(actual.getAnnotationUri(), expected.getAnnotationUri());
    assertEquals(actual.getArticleDoi(), expected.getArticleDoi());
    assertEquals(actual.getArticleTitle(), expected.getArticleTitle());
    assertEquals(actual.getParentID(), expected.getParentID());
    assertEquals(actual.getBody(), expected.getBody());
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
