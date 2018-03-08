package org.ambraproject.rhino.service.impl;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.MessageSender;
import org.ambraproject.rhino.service.SolrIndexService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link SolrIndexServiceImpl}.
 */
@ContextConfiguration(classes = SolrIndexServiceTest.class)
@Configuration
public class SolrIndexServiceTest extends AbstractStubbingArticleTest {

  private ArticleCrudService mockArticleCrudService;

  private MessageSender mockMessageSender;
  
  private SolrIndexService mockSolrIndexService;

  private Doi expectedDoi;

  private ArticleIdentifier expectedArticleIdentifier;

  /**
   * Prepare test fixtures.
   */
  @BeforeMethod(alwaysRun = true)
  protected void init() {
    mockArticleCrudService = applicationContext.getBean(ArticleCrudService.class);
    mockMessageSender = applicationContext.getBean(MessageSender.class);
    mockSolrIndexService = applicationContext.getBean(SolrIndexService.class);

    expectedDoi = createStubArticleDoi();

    expectedArticleIdentifier = ArticleIdentifier.create(expectedDoi);
  }

  @Bean
  public AssetCrudService assetCrudService() {
    AssetCrudService mockAssetCrudService = spy(AssetCrudServiceImpl.class);
    LOG.info("assetCrudService() * --> {}", mockAssetCrudService);
    return mockAssetCrudService;
  }

  @Bean
  protected ArticleCrudService articleCrudService() {
    final ArticleCrudService articleCrudService = spy(ArticleCrudServiceImpl.class);
    return articleCrudService;
  }

  @Bean
  protected MessageSender messageSender() {
    final MessageSender messageSender = mock(MessageSender.class);
    return messageSender;
  }

  @Bean
  protected SolrIndexService solrIndexService() {
    final SolrIndexService solrIndexService = spy(SolrIndexServiceImpl.class);
    return solrIndexService;
  }
  /**
   * Test successful Solr index. 
   */
  @Test
  public void testUpdateSolrIndexShouldSucceed() {
    final Article expectedArticle = createStubArticle();
    final ArticleIngestion expectedIngestion = createStubArticleIngestion(
        expectedArticle, INGESTION_NUMBER);
    
    final ArticleRevision expectedRevision = createStubArticleRevision(
        REVISION_ID, REVISION_NUMBER);
    expectedRevision.setIngestion(expectedIngestion);

    doReturn(expectedArticle).when(mockArticleCrudService).readArticle(expectedArticleIdentifier);
    doReturn(expectedRevision).when(mockArticleCrudService).readLatestRevision(expectedArticle);
  }
}
