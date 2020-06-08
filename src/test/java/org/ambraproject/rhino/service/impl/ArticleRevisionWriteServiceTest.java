package org.ambraproject.rhino.service.impl;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import com.google.cloud.storage.Storage;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleRevisionWriteService;
import org.hibernate.Query;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * Unit tests for {@link ArticleRevisionWriteServiceImpl}.
 */
@ContextConfiguration(classes = ArticleRevisionWriteServiceTest.class)
@Configuration
public class ArticleRevisionWriteServiceTest extends AbstractStubbingArticleTest {

  @Bean
  public Storage storage() throws Exception {
    return mock(Storage.class);
  }

  @Bean
  public ArticleCrudService articleCrudService() {
    final ArticleCrudService articleCrudService = spy(new GCSArticleCrudServiceImpl());
    return articleCrudService;
  }

  @Bean
  public ArticleRevisionWriteService articleRevisionWriteService() {
    final ArticleRevisionWriteService articleRevisionWriteService = spy(new ArticleRevisionWriteServiceImpl());
    return articleRevisionWriteService;
  }

  private Query mockQuery;

  private ArticleRevisionWriteService mockArticleRevisionWriteService;

  private ArticleCrudService mockArticleCrudService;

  private Doi expectedDoi;

  private Article expectedArticle;

  private ArticleIngestionIdentifier expectedIngestionIdentifier;

  private ArticleIngestion expectedArticleIngestion;

  private ArticleRevision expectedArticleRevision;

  private ArticleRevisionIdentifier expectedRevisionIdentifier;

  /**
   * Creates an instance of <code>ArticleRevisionWriteServiceTest</code>.
   */
  public ArticleRevisionWriteServiceTest() {
    super(true /* spyOnHibernateTemplate */);
  }

  /**
   * Prepare test fixtures.
   */
  @Before
  public void init() {
    mockQuery = mock(Query.class);

    mockArticleRevisionWriteService = applicationContext.getBean(ArticleRevisionWriteService.class);

    mockArticleCrudService = applicationContext.getBean(ArticleCrudService.class);

    expectedDoi = createStubArticleDoi();

    expectedArticle = createStubArticle();

    expectedIngestionIdentifier = createStubArticleIngestionIdentifier(expectedDoi, INGESTION_NUMBER);

    expectedArticleIngestion = createStubArticleIngestion(expectedArticle, INGESTION_NUMBER);

    expectedArticleRevision = createStubArticleRevision(REVISION_ID, REVISION_NUMBER);
    expectedArticleRevision.setIngestion(expectedArticleIngestion);

    expectedRevisionIdentifier = createStubArticleRevisionIdentifier(expectedDoi, REVISION_NUMBER);
  }

  /**
   * Returns a mock query used to mock a HibernateTemplate.
   */
  private Answer<Query> createQueryAnswer() {
    return invocation -> {
      final String sql = invocation.getArgument(0);

      // Return appropriate data based on SQL.
      LOG.info("sql: {}", sql);
      if (sql.contains("SELECT MAX(rev.revisionNumber)")) {
        when(mockQuery.uniqueResult()).thenReturn(REVISION_NUMBER);
      } else if (sql.contains("FROM ArticleIngestion")) {
        when(mockQuery.uniqueResult()).thenReturn(expectedArticleIngestion);
      } else if (sql.contains("FROM ArticleRevision")) {
        when(mockQuery.uniqueResult()).thenReturn(expectedArticleRevision);
      }
      return mockQuery;
    };
  }

  /**
   * Test successful creation of an article revision.
   */
  @Test
  @DirtiesContext
  public void testCreateRevisionShouldSucceed() {
    final Answer<Query> answer = createQueryAnswer();
    final HibernateTemplate mockHibernateTemplate = buildMockHibernateTemplate(answer);

    // Create a new ArticleRevision, as
    // ArticleRevisionWriteServiceImpl.createRevision()
    // will create a new instance prior to calling
    // ArticleRevisionWriteServiceImpl.refreshArticleRelationships().
    final ArticleRevision refreshedArticleRevision = new ArticleRevision();
    refreshedArticleRevision.setIngestion(expectedArticleIngestion);
    refreshedArticleRevision.setRevisionNumber(REVISION_NUMBER + 1);

    doNothing().when(mockArticleCrudService).refreshArticleRelationships(refreshedArticleRevision);

    final ArticleRevision actualRevision = mockArticleRevisionWriteService.createRevision(expectedIngestionIdentifier);

    assertThat(actualRevision).isEqualTo(refreshedArticleRevision);

    verify(mockArticleCrudService).refreshArticleRelationships(refreshedArticleRevision);
    verify(mockHibernateTemplate, times(3)).execute(any());

    verify(mockQuery).setParameter("doi", expectedArticle.getDoi());

    verify(mockQuery).setParameter("doi", expectedDoi.getName());

    verify(mockQuery).setParameter("ingestionNumber", expectedIngestionIdentifier.getIngestionNumber());
  }

  /**
   * Test successful update of an article revision.
   */
  @Test
  @DirtiesContext
  public void testWriteRevisionShouldSucceed() {
    final Answer<Query> answer = createQueryAnswer();
    final HibernateTemplate mockHibernateTemplate = buildMockHibernateTemplate(answer);

    doNothing().when(mockArticleCrudService).refreshArticleRelationships(expectedArticleRevision);

    final ArticleRevision actualRevision = mockArticleRevisionWriteService.writeRevision(expectedRevisionIdentifier,
        expectedIngestionIdentifier);

    assertThat(actualRevision).isEqualTo(expectedArticleRevision);

    verify(mockArticleCrudService).refreshArticleRelationships(expectedArticleRevision);
    verify(mockHibernateTemplate, times(4)).execute(any());
    verify(mockHibernateTemplate).saveOrUpdate(expectedArticleRevision);

    verify(mockQuery, times(2)).setParameter("doi", expectedDoi.getName());

    verify(mockQuery).setParameter("ingestionNumber", expectedIngestionIdentifier.getIngestionNumber());

    verify(mockQuery).setParameter("revisionNumber", expectedRevisionIdentifier.getRevision());
  }

  /**
   * Test successful update of an article revision, where article doesn't have a
   * previous version.
   */
  @Test
  @DirtiesContext
  public void testWriteRevisionWithouPriorShouldSucceed() {
    final HibernateTemplate mockHibernateTemplate = buildMockHibernateTemplate();

    final Optional<ArticleRevision> latestRevision = Optional.empty();

    final ArticleRevision newRevision = new ArticleRevision();
    newRevision.setRevisionNumber(expectedRevisionIdentifier.getRevision());
    newRevision.setIngestion(expectedArticleIngestion);

    doReturn(expectedArticleIngestion).when(mockArticleCrudService).readIngestion(expectedIngestionIdentifier);
    doReturn(expectedArticleRevision).when(mockArticleCrudService).readRevision(expectedRevisionIdentifier);
    doReturn(latestRevision).when(mockArticleCrudService).getLatestRevision(expectedArticle);
    doNothing().when(mockArticleCrudService).refreshArticleRelationships(newRevision);

    final ArticleRevision actualRevision = mockArticleRevisionWriteService.writeRevision(expectedRevisionIdentifier,
        expectedIngestionIdentifier);

    assertThat(actualRevision).isEqualTo(newRevision);

    verify(mockHibernateTemplate).saveOrUpdate(newRevision);
    verify(mockArticleCrudService).refreshArticleRelationships(newRevision);
  }

  /**
   * Test successful deletion of the latest article revision.
   */
  @Test
  @DirtiesContext
  public void testDeleteLatestRevisionShouldSucceed() {
    final Answer<Query> answer = createQueryAnswer();
    final HibernateTemplate mockHibernateTemplate = buildMockHibernateTemplate(answer);

    doNothing().when(mockArticleCrudService).refreshArticleRelationships(expectedArticleRevision);

    mockArticleRevisionWriteService.deleteRevision(expectedRevisionIdentifier);

    verify(mockArticleCrudService).refreshArticleRelationships(expectedArticleRevision);
    verify(mockHibernateTemplate).delete(expectedArticleRevision);
    verify(mockHibernateTemplate, times(5)).execute(any());
    verify(mockQuery, times(5)).uniqueResult();
  }

  /**
   * Test successful deletion of a revision, for an article with multiple
   * revisions.
   */
  @Test
  @DirtiesContext
  public void testDeleteNonLatestRevisionShouldSucceed() {
    final HibernateTemplate mockHibernateTemplate = buildMockHibernateTemplate();

    final Optional<ArticleRevision> latestRevision = Optional
        .of(createStubArticleRevision(1001L /* revisionId */, 5 /* revisionNumber */));

    doReturn(expectedArticleRevision).when(mockArticleCrudService).readRevision(expectedRevisionIdentifier);
    doReturn(latestRevision).when(mockArticleCrudService).getLatestRevision(expectedArticle);

    mockArticleRevisionWriteService.deleteRevision(expectedRevisionIdentifier);

    verify(mockArticleCrudService, times(0)).refreshArticleRelationships(any());
    verify(mockHibernateTemplate).delete(expectedArticleRevision);
  }
}
