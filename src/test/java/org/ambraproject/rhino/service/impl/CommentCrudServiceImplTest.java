package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.CommentIdentifier;
import org.ambraproject.rhino.model.Comment;
import org.ambraproject.rhino.model.Flag;
import org.ambraproject.rhino.model.FlagReasonCode;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.CommentCrudService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.view.comment.CommentCountView;
import org.ambraproject.rhino.view.comment.CommentFlagInputView;
import org.ambraproject.rhino.view.comment.CommentInputView;
import org.ambraproject.rhino.view.comment.CommentNodeView;
import org.mockito.internal.stubbing.answers.Returns;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = CommentCrudServiceImplTest.class)
@Configuration
public class CommentCrudServiceImplTest extends AbstractStubbingArticleTest {

  private CommentCrudService mockCommentCrudService;

  private ArticleCrudService mockArticleCrudService;

  private HibernateTemplate mockHibernateTemplate;

  private RuntimeConfiguration mockRuntimeConfiguration;

  private CommentNodeView.Factory mockCommentNodeViewFactory;

  private JournalCrudService mockJournalCrudService;

  private List<Comment> stubComments = ImmutableList.of(createStubComment());

  private List<Flag> stubFlags = ImmutableList.of(createStubFlag());

  private CommentInputView stubCommentInputView;

  private final String COMMENT_CREATION_JSON = "{\"creatorUserId\": 10365, \"parentCommentId\": \"10.1371/annotation/0043aae2-f69d-4a05-ab19-4709704eb749\", \"title\": \"no, really watch this\", \"body\": \"http://www.youtube.com/watch?v=iGQdwSAAz9I\", \"highlightedText\": \"whoah...\", \"competingInterestStatement\": \"I'm going for an Emmy\"}";

  private CommentFlagInputView stubFlagInputView;

  private final String FLAG_CREATION_JSON = "{\"creatorUserId\": 10365, \"body\": \"oops\", \"reasonCode\": \"spam\"}";

  @Before
  public void initMocks() throws IllegalAccessException, NoSuchFieldException {
    mockCommentCrudService = applicationContext.getBean(CommentCrudService.class);
    mockArticleCrudService = applicationContext.getBean(ArticleCrudService.class);
    mockHibernateTemplate = applicationContext.getBean(HibernateTemplate.class);
    reset(mockHibernateTemplate);
    mockRuntimeConfiguration = applicationContext.getBean(RuntimeConfiguration.class);
    mockJournalCrudService = applicationContext.getBean(JournalCrudService.class);
    Gson mockEntityGson = applicationContext.getBean(Gson.class);
    stubCommentInputView = mockEntityGson.fromJson(COMMENT_CREATION_JSON, CommentInputView.class);
    stubFlagInputView = mockEntityGson.fromJson(FLAG_CREATION_JSON, CommentFlagInputView.class);
  }

  @Bean
  public CommentCrudService commentCrudService() {
    mockCommentCrudService = spy(CommentCrudServiceImpl.class);
    LOG.debug("commentCrudService() * --> {}", mockCommentCrudService);
    return mockCommentCrudService;
  }

  @Bean
  public CommentNodeView.Factory commentNodeViewFactory() {
    mockCommentNodeViewFactory = mock(CommentNodeView.Factory.class);
    LOG.debug("commentNodeView.Factory() * --> {}", mockCommentNodeViewFactory);
    return mockCommentNodeViewFactory;
  }

  private Comment createStubComment() {
    final Comment comment = new Comment();
    comment.setArticle(createStubArticle());
    comment.setCreated(new Date());
    return comment;
  }

  private Flag createStubFlag() {
    final Flag flag = new Flag(0L, FlagReasonCode.OTHER, createStubComment());
    flag.setLastModified(new Date());
    return flag;
  }

  @Test
  public void testServeComments() throws Exception {
    when(mockArticleCrudService.readArticle(ArticleIdentifier.create("0"))).thenReturn(createStubArticle());

    when(mockHibernateTemplate.find(any(String.class), any(Object.class))).thenAnswer(new Returns(stubComments));
    mockCommentCrudService.serveComments(ArticleIdentifier.create("0"));
  }

  @Test
  public void testServeComment() throws Exception {
    when(mockHibernateTemplate.execute(any())).thenReturn(createStubComment());
    when(mockHibernateTemplate.find(any(String.class), any(Object.class))).thenAnswer(new Returns(stubComments));

    mockCommentCrudService.serveComment(CommentIdentifier.create("0"));
  }

  @Test
  public void testCreateComment() throws Exception {
    final ArticleIdentifier articleIdentifier = ArticleIdentifier.create("10.1371/journal.pbio.2001414");
    when(mockArticleCrudService.readArticle(articleIdentifier)).thenReturn(createStubArticle());
    when(mockHibernateTemplate.execute(any())).thenReturn(createStubComment());
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
    when(mockHibernateTemplate.execute(any())).thenReturn(createStubComment()).thenAnswer(new Returns(stubFlags));

    mockCommentCrudService.removeFlagsFromComment(CommentIdentifier.create("0"));

    verify(mockHibernateTemplate).deleteAll(stubFlags);
  }

  @Test
  public void testCreateCommentFlag() throws Exception {
    when(mockHibernateTemplate.execute(any())).thenReturn(createStubComment());

    mockCommentCrudService.createCommentFlag(CommentIdentifier.create("0"), stubFlagInputView);

    verify(mockHibernateTemplate).save(any(Flag.class));
  }

  @Test
  public void testReadAllCommentFlags() throws Exception {
    when(mockHibernateTemplate.execute(any())).thenAnswer(new Returns(stubFlags));

    mockCommentCrudService.readAllCommentFlags();
  }

  @Test
  public void testReadCommentFlag() throws Exception {
    when(mockHibernateTemplate.execute(any())).thenReturn(createStubFlag());
    mockCommentCrudService.readCommentFlag(0L);
  }

  @Test
  public void testReadCommentFlagsOn() throws Exception {
    when(mockHibernateTemplate.execute(any())).thenReturn(createStubComment()).thenAnswer(new Returns(stubFlags));
    mockCommentCrudService.readCommentFlagsOn(CommentIdentifier.create("0"));
  }

  @Test
  public void testReadCommentFlagsForJournal() throws Exception {
    when(mockJournalCrudService.readJournal("test")).thenReturn(new Journal("test"));
    when(mockHibernateTemplate.execute(any())).thenAnswer(new Returns(stubFlags));
    mockCommentCrudService.readCommentFlagsForJournal("test");
  }

  @Test
  public void testDeleteCommentFlag() throws Exception {
    when(mockHibernateTemplate.execute(any())).thenReturn(createStubFlag());

    mockCommentCrudService.deleteCommentFlag(0L);

    verify(mockHibernateTemplate).delete(any(Flag.class));
  }

  @Test
  public void testServeFlaggedComments() throws Exception {
    when(mockHibernateTemplate.execute(any())).thenReturn(stubComments);
    mockCommentCrudService.serveFlaggedComments();
  }

  @Test
  public void testGetCommentCount() throws Exception {
    when(mockHibernateTemplate.execute(any())).thenReturn(new CommentCountView(1, 1, 0));
    mockCommentCrudService.getCommentCount(createStubArticle());
  }

  @Test
  public void testGetCommentsCreatedOn() throws Exception {
    when(mockHibernateTemplate.execute(any())).thenReturn(ImmutableList.of());
    mockCommentCrudService.getCommentsCreatedOn(LocalDate.now());
  }

  @Test
  public void testReadRecentComments() throws Exception {
    when(mockHibernateTemplate.execute(any())).thenReturn(ImmutableList.of());
    mockCommentCrudService.getCommentsCreatedOn(LocalDate.now());
  }

}
