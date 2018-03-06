package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import org.ambraproject.rhino.AbstractRhinoTest;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.CommentIdentifier;
import org.ambraproject.rhino.model.Comment;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.CommentCrudService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.view.comment.CommentInputView;
import org.ambraproject.rhino.view.comment.CommentNodeView;
import org.mockito.internal.stubbing.answers.Returns;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = CommentCrudServiceImplTest.class)
@Configuration
public class CommentCrudServiceImplTest extends AbstractRhinoTest {

  private CommentCrudService mockCommentCrudService;

  private ArticleCrudService mockArticleCrudService;

  private HibernateTemplate mockHibernateTemplate;

  private RuntimeConfiguration mockRuntimeConfiguration;

  private CommentNodeView.Factory mockCommentNodeViewFactory;

  private JournalCrudService mockJournalCrudService;

  private List<Comment> stubComments = ImmutableList.of(createStubComment());

  private CommentInputView stubCommentInputView;

  private final String COMMENT_CREATION_JSON = "{\"creatorUserId\": 10365, \"parentCommentId\": \"10.1371/annotation/0043aae2-f69d-4a05-ab19-4709704eb749\", \"title\": \"no, really watch this\", \"body\": \"http://www.youtube.com/watch?v=iGQdwSAAz9I\", \"highlightedText\": \"whoah...\", \"competingInterestStatement\": \"I'm going for an Emmy\"}";

  @BeforeMethod
  public void initMocks() throws IllegalAccessException, NoSuchFieldException {
    mockCommentCrudService = applicationContext.getBean(CommentCrudService.class);
    mockArticleCrudService = applicationContext.getBean(ArticleCrudService.class);
    mockHibernateTemplate = applicationContext.getBean(HibernateTemplate.class);
    mockRuntimeConfiguration = applicationContext.getBean(RuntimeConfiguration.class);
    Gson mockEntityGson = applicationContext.getBean(Gson.class);
    stubCommentInputView = mockEntityGson.fromJson(COMMENT_CREATION_JSON, CommentInputView.class);
  }

  @Bean
  public CommentCrudService commentCrudService() {
    mockCommentCrudService = spy(CommentCrudServiceImpl.class);
    LOG.debug("commentCrudService() * --> {}", mockCommentCrudService);
    return mockCommentCrudService;
  }

  @Bean
  public ArticleCrudService articleCrudService() {
    mockArticleCrudService = mock(ArticleCrudServiceImpl.class);
    LOG.debug("articleCrudService() * --> {}", mockArticleCrudService);
    return mockArticleCrudService;
  }

  @Bean
  public CommentNodeView.Factory commentNodeViewFactory() {
    mockCommentNodeViewFactory = mock(CommentNodeView.Factory.class);
    LOG.debug("commentNodeView.Factory() * --> {}", mockCommentNodeViewFactory);
    return mockCommentNodeViewFactory;
  }

  @Bean
  public JournalCrudService journalCrudService() {
    mockJournalCrudService = mock(JournalCrudService.class);
    LOG.debug("journalCrudService() * --> {}", mockJournalCrudService);
    return mockJournalCrudService;
  }

  private Comment createStubComment() {
    final Comment comment = new Comment();
    comment.setArticle(createStubArticle());
    comment.setCreated(new Date());
    return comment;
  }

  @Test
  public void testServeComments() throws Exception {
    when(mockArticleCrudService.readArticle(ArticleIdentifier.create("0"))).thenReturn(createStubArticle());

    when(mockHibernateTemplate.find(any(String.class), any(Object.class))).thenAnswer(new Returns(stubComments));
    when(mockRuntimeConfiguration.getCompetingInterestPolicyStart()).thenReturn(LocalDate.now());

    mockCommentCrudService.serveComments(ArticleIdentifier.create("0"));
  }

  @Test
  public void testServeComment() throws Exception {
    when(mockHibernateTemplate.execute(any())).thenReturn(createStubComment());
    when(mockRuntimeConfiguration.getCompetingInterestPolicyStart()).thenReturn(LocalDate.now());
    when(mockHibernateTemplate.find(any(String.class), any(Object.class))).thenAnswer(new Returns(stubComments));

    mockCommentCrudService.serveComment(CommentIdentifier.create("0"));
  }

  @Test
  public void testCreateComment() throws Exception {
    final ArticleIdentifier articleIdentifier = ArticleIdentifier.create("10.1371/journal.pbio.2001414");
    when(mockArticleCrudService.readArticle(articleIdentifier)).thenReturn(createStubArticle());
    when(mockHibernateTemplate.execute(any())).thenReturn(createStubComment());
    when(mockRuntimeConfiguration.getCompetingInterestPolicyStart()).thenReturn(LocalDate.now());

    doAnswer(invocation -> {
      Object[] args = invocation.getArguments();
      ((Comment) args[0]).setCreated(new Date());
      return null; // void method, so return null
    }).when(mockHibernateTemplate).save(any(Comment.class));

    mockCommentCrudService.createComment(Optional.of(articleIdentifier), stubCommentInputView);

    verify(mockHibernateTemplate).save(any());
  }

  @Test
  public void testPatchComment() throws Exception {
    when(mockHibernateTemplate.execute(any())).thenReturn(createStubComment());
    when(mockRuntimeConfiguration.getCompetingInterestPolicyStart()).thenReturn(LocalDate.now());

    final CommentIdentifier commentId = CommentIdentifier.create("10.1371/annotation/0043aae2-f69d-4a05-ab19-4709704eb749");
    mockCommentCrudService.patchComment(commentId, stubCommentInputView);

    verify(mockHibernateTemplate).update(any());
  }

  @Test
  public void testDeleteComment() throws Exception {
    when(mockHibernateTemplate.execute(any())).thenReturn(createStubComment());

    mockCommentCrudService.deleteComment(CommentIdentifier.create("10.1371/annotation/0043aae2-f69d-4a05-ab19-4709704eb749"));

    verify(mockHibernateTemplate).delete(any(Comment.class));
  }

  @Test
  public void testRemoveFlagsFromComment() throws Exception {
  }

  @Test
  public void testCreateCommentFlag() throws Exception {
  }

  @Test
  public void testReadAllCommentFlags() throws Exception {
  }

  @Test
  public void testReadCommentFlag() throws Exception {
  }

  @Test
  public void testReadCommentFlagsOn() throws Exception {
  }

  @Test
  public void testReadCommentFlagsForJournal() throws Exception {
  }

  @Test
  public void testDeleteCommentFlag() throws Exception {
  }

  @Test
  public void testServeFlaggedComments() throws Exception {
  }

  @Test
  public void testGetCommentCount() throws Exception {
  }

  @Test
  public void testGetCommentsCreatedOn() throws Exception {
  }

  @Test
  public void testReadRecentComments() throws Exception {
  }

}