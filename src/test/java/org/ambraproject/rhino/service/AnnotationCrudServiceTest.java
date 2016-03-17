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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.ambraproject.models.Annotation;
import org.ambraproject.models.AnnotationType;
import org.ambraproject.models.Article;
import org.ambraproject.models.UserProfile;
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.IngestibleUtil;
import org.ambraproject.rhino.RhinoTestHelper;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.util.Archive;
import org.ambraproject.rhino.view.comment.AnnotationOutputView;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  @Autowired
  private RuntimeConfiguration runtimeConfiguration;

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
    InputStream mockIngestible = IngestibleUtil.buildMockIngestible(input, ImmutableList.of());
    Archive archive = Archive.readZipFileIntoMemory(doiStub + ".zip", mockIngestible);
    Article article = articleCrudService.writeArchive(archive, Optional.of(articleId),
        DoiBasedCrudService.WriteMode.CREATE_ONLY);
    article.setJournals(ImmutableSet.of());

    UserProfile creator = new UserProfile("fake@example.org", "displayName", "password");
    hibernateTemplate.save(creator);
    Annotation comment1 = new Annotation();
    comment1.setCreator(creator);
    comment1.setArticleID(article.getID());
    comment1.setAnnotationUri("10.1371/annotation/test_comment_1");
    comment1.setType(AnnotationType.COMMENT);
    comment1.setTitle("Test Comment One");
    comment1.setBody("Test Comment One Body");
    hibernateTemplate.save(comment1);
    Date commentCreated = new Date();

    // Reply to the comment.
    Annotation reply = new Annotation();
    reply.setCreator(creator);
    reply.setArticleID(article.getID());
    reply.setAnnotationUri("10.1371/reply/test_reply_level_1");
    reply.setParentID(comment1.getID());
    reply.setType(AnnotationType.REPLY);
    reply.setTitle("Test Reply Level 1");
    reply.setBody("Test Reply Level 1 Body");
    hibernateTemplate.save(reply);

    // Another first-level reply to the comment.
    Annotation reply2 = new Annotation();
    reply2.setCreator(creator);
    reply2.setArticleID(article.getID());
    reply2.setAnnotationUri("10.1371/reply/test_reply_2_level_1");
    reply2.setParentID(comment1.getID());
    reply2.setType(AnnotationType.REPLY);
    reply2.setTitle("Test Reply 2 Level 1");
    reply2.setBody("Test Reply 2 Level 1 Body");
    hibernateTemplate.save(reply2);

    // Reply to the first reply.
    Annotation reply3 = new Annotation();
    reply3.setCreator(creator);
    reply3.setArticleID(article.getID());
    reply3.setAnnotationUri("10.1371/reply/test_reply_3_level_2");
    reply3.setParentID(reply.getID());
    reply3.setType(AnnotationType.REPLY);
    reply3.setTitle("Test Reply 3 Level 2");
    reply3.setBody("Test Reply 3 Level 2 Body");
    hibernateTemplate.save(reply3);

    Annotation comment2 = new Annotation();
    comment2.setCreator(creator);
    comment2.setArticleID(article.getID());
    comment2.setAnnotationUri("10.1371/annotation/test_comment_2");
    comment2.setType(AnnotationType.COMMENT);
    comment2.setTitle("Test Comment Two");
    comment2.setBody("Test Comment Two Body");
    hibernateTemplate.save(comment2);

    Annotation comment3 = new Annotation();
    comment3.setCreator(creator);
    comment3.setArticleID(article.getID());
    comment3.setAnnotationUri("10.1371/annotation/test_comment_3");
    comment3.setType(AnnotationType.COMMENT);
    comment3.setTitle("Test Comment");
    comment3.setBody("Test Comment Body");
    hibernateTemplate.save(comment3);

    String json = annotationCrudService.readComments(articleId).readJson(entityGson);
    assertTrue(json.length() > 0);

    // Confirm that date strings in the JSON are formatted as ISO8601 ("2012-04-23T18:25:43.511Z").
    // We have to do this at a lower level since AnnotationView exposes the created field as
    // a Java Date instead of a String.
    List<Map<String, ?>> actualAnnotations = entityGson.fromJson(json,
        new TypeToken<List<Map<String, ?>>>() {
        }.getType()
    );
    assertEquals(actualAnnotations.size(), 3);
    for (Map<String, ?> commentMap : actualAnnotations) {
      String createdStr = (String) commentMap.get("created");

      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'");
      dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

      // TODO: this might fail near midnight, if we're unlucky and the hibernate
      // entity gets saved just before midnight, and this is executed just after.
      assertTrue(createdStr.startsWith(dateFormat.format(commentCreated)), createdStr);
    }

    AnnotationOutputView.Factory factory = new AnnotationOutputView.Factory(runtimeConfiguration, article,
        ImmutableList.of(comment1, comment2, comment3, reply, reply2, reply3));

    assertAnnotationsEqual(actualAnnotations.get(0), factory.buildView(comment1));
    assertAnnotationsEqual(actualAnnotations.get(1), factory.buildView(comment2));

    // Comment with no replies.
    assertAnnotationsEqual(actualAnnotations.get(2), factory.buildView(comment3));
  }

  private static final ImmutableSet<String> ANNOTATION_FIELDS = ImmutableSet.of(
      "type", "annotationUri", "title", "body", "created", "lastModified", "highlightedText",
      "competingInterestStatement", "replyTreeSize", "mostRecentActivity");
  private static final ImmutableSet<String> CREATOR_FIELDS = ImmutableSet.of("profileUri", "authId");
  private static final ImmutableSet<String> ARTICLE_FIELDS = ImmutableSet.of("doi", "state");

  private static <K> void assertMapsEqual(Map<K, ?> actual, Map<K, ?> expected, Collection<? extends K> keys) {
    for (K key : keys) {
      assertEquals(actual.get(key), expected.get(key), key.toString());
    }
  }

  private void assertAnnotationsEqual(Map<String, ?> actualRepr, AnnotationOutputView expectedObj) {
    Map<String, ?> expectedRepr = entityGson.fromJson(entityGson.toJson(expectedObj), Map.class);
    assertAnnotationsEqual(actualRepr, expectedRepr);
  }

  private static void assertAnnotationsEqual(Map<String, ?> actual, Map<String, ?> expected) {
    assertMapsEqual(actual, expected, ANNOTATION_FIELDS);

    Map<String, ?> actualCreator = (Map<String, ?>) actual.get("creator");
    Map<String, ?> expectedCreator = (Map<String, ?>) expected.get("creator");
    assertMapsEqual(actualCreator, expectedCreator, CREATOR_FIELDS);

    Map<String, ?> actualArticle = (Map<String, ?>) actual.get("parentArticle");
    Map<String, ?> expectedArticle = (Map<String, ?>) expected.get("parentArticle");
    assertMapsEqual(actualArticle, expectedArticle, ARTICLE_FIELDS);

    List<Map<String, ?>> actualReplies = (List<Map<String, ?>>) actual.get("replies");
    List<Map<String, ?>> expectedReplies = (List<Map<String, ?>>) expected.get("replies");
    assertEquals(actualReplies.size(), expectedReplies.size());
    for (int i = 0; i < expectedReplies.size(); i++) {
      assertAnnotationsEqual(actualReplies.get(i), expectedReplies.get(i));
    }
  }

}
