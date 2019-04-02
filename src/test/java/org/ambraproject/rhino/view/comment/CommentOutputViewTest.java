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

package org.ambraproject.rhino.view.comment;

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.Comment;
import org.junit.Before;
import org.junit.Test;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertNotNull;


public class CommentOutputViewTest {

  private RuntimeConfiguration runtimeConfiguration = mock(RuntimeConfiguration.class);

  private CommentOutputView.Factory factory;

  @Before
  public void init() {

    when(runtimeConfiguration.getCompetingInterestPolicyStart())
        .thenReturn(LocalDate.now(ZoneId.systemDefault()));

    Comment comment = createStubComment();
    factory = new CommentOutputView.Factory(new CompetingInterestPolicy(runtimeConfiguration),
        ImmutableList.of(comment), comment.getArticle());

  }

  private Comment createStubComment() {
    Comment comment = new Comment();
    Article article = new Article();
    article.setDoi("test");
    comment.setArticle(article);
    comment.setBody("test body");
    comment.setCommentId(0L);
    comment.setCreated(Date.from(Instant.now()));
    return comment;
  }

  @Test
  public void testBuildView() throws Exception {
    CommentOutputView commentOutputView = factory.buildView(createStubComment());
    assertNotNull(commentOutputView);
  }

}