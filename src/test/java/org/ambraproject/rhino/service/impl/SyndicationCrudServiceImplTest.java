package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.Syndication;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.service.MessageSender;
import org.ambraproject.rhino.service.SyndicationCrudService;
import org.mockito.internal.stubbing.answers.Returns;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = SyndicationCrudServiceImplTest.class)
@Configuration
public class SyndicationCrudServiceImplTest extends AbstractStubbingArticleTest {

  private SyndicationCrudService mockSyndicationCrudService;

  private MessageSender mockMessageSender;

  private RuntimeConfiguration mockRuntimeConfiguration;

  private JournalCrudService mockJournalCrudService;

  private ArticleCrudService mockArticleCrudService;

  private HibernateTemplate mockHibernateTemplate;

  private List<Syndication> stubSyndications = ImmutableList.of(createStubSyndication());

  private final ArticleRevisionIdentifier stubRevisionId = ArticleRevisionIdentifier.create("0", 1);

  @Bean
  public SyndicationCrudService syndicationCrudService() {
    mockSyndicationCrudService = spy(SyndicationCrudServiceImpl.class);
    LOG.debug("syndicationCrudService() * --> {}", mockSyndicationCrudService);
    return mockSyndicationCrudService;
  }

  @Bean
  public MessageSender messageSender() {
    mockMessageSender = mock(MessageSender.class);
    LOG.debug("messageSenderCrudService() * --> {}", mockMessageSender);
    return mockMessageSender;
  }

  private Syndication createStubSyndication() {
    return new Syndication(createStubArticleRevision(), "test");
  }

  @Before
  public void initMocks() throws IllegalAccessException, NoSuchFieldException {
    mockSyndicationCrudService = applicationContext.getBean(SyndicationCrudService.class);
    mockArticleCrudService = applicationContext.getBean(ArticleCrudService.class);
    mockRuntimeConfiguration = applicationContext.getBean(RuntimeConfiguration.class);
    mockJournalCrudService = applicationContext.getBean(JournalCrudService.class);
    mockHibernateTemplate = applicationContext.getBean(HibernateTemplate.class);
    reset(mockHibernateTemplate);
    mockMessageSender = applicationContext.getBean(MessageSender.class);
  }

  @Test
  public void testGetSyndication() throws Exception {
    when(mockArticleCrudService.readRevision(stubRevisionId)).thenReturn(createStubArticleRevision());
    when(mockHibernateTemplate.execute(any())).thenReturn(createStubSyndication());

    mockSyndicationCrudService.getSyndication(stubRevisionId, "test");
  }

  @Test
  public void testGetSyndications() throws Exception {
    when(mockArticleCrudService.readRevision(stubRevisionId)).thenReturn(createStubArticleRevision());
    when(mockHibernateTemplate.execute(any())).thenAnswer(new Returns(stubSyndications));

    mockSyndicationCrudService.getSyndications(stubRevisionId);
  }

  @Test
  public void testUpdateSyndication() throws Exception {
    when(mockArticleCrudService.readRevision(stubRevisionId)).thenReturn(createStubArticleRevision());
    when(mockHibernateTemplate.execute(any())).thenReturn(createStubSyndication());

    mockSyndicationCrudService.updateSyndication(stubRevisionId, "test", "test", "test");

    verify(mockHibernateTemplate).update(any(Syndication.class));
  }

  @Test
  public void testCreateSyndication() throws Exception {
    when(mockArticleCrudService.readRevision(stubRevisionId)).thenReturn(createStubArticleRevision());

    mockSyndicationCrudService.createSyndication(stubRevisionId, "test");

    verify(mockHibernateTemplate).save(any(Syndication.class));
  }

  @Test
  public void testReadSyndications() throws Exception {
    when(mockRuntimeConfiguration.getQueueConfiguration()).thenReturn(stubConfig);
    when(mockJournalCrudService.readJournal("test")).thenReturn(new Journal("test"));
    when(mockHibernateTemplate.execute(any())).thenAnswer(new Returns(stubSyndications));

    mockSyndicationCrudService.readSyndications("test", ImmutableList.of("test"));
  }

  @Test
  @DirtiesContext
  public void testSyndicateExistingSyndication() throws Exception {
    when(mockArticleCrudService.readRevision(stubRevisionId)).thenReturn(createStubArticleRevision());
    when(mockHibernateTemplate.execute(any())).thenReturn(createStubSyndication());

    mockSyndicationCrudService.syndicate(stubRevisionId, "test");

    verify(mockHibernateTemplate).update(any(Syndication.class));
    verify(mockMessageSender).sendBody(any(String.class), any(String.class));
  }

  @Test
  @DirtiesContext
  public void testSyndicateNoSyndication() throws Exception {
    when(mockArticleCrudService.readRevision(stubRevisionId)).thenReturn(createStubArticleRevision());
    when(mockHibernateTemplate.execute(any())).thenReturn(null);

    mockSyndicationCrudService.syndicate(stubRevisionId, "test");

    verify(mockHibernateTemplate).save(any(Syndication.class));
    verify(mockMessageSender).sendBody(any(String.class), any(String.class));
  }

  private final RuntimeConfiguration.QueueConfiguration stubConfig = new RuntimeConfiguration.QueueConfiguration() {
    @Override
    public String getBrokerUrl() {
      return "test";
    }

    @Override
    public String getSolrUpdate() {
      return "test";
    }

    @Override
    public String getLiteSolrUpdate() {
      return "test";
    }

    @Override
    public String getSolrDelete() {
      return "test";
    }

    @Override
    public int getSyndicationRange() {
      return 0;
    }
  };
}
