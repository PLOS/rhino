package org.ambraproject.rhino.view.comment;

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.Comment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotNull;


public class CommentOutputViewTest {

  private RuntimeConfiguration runtimeConfiguration = mock(RuntimeConfiguration.class);

  private CommentOutputView.Factory factory;

  @BeforeMethod
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