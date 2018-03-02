package org.ambraproject.rhino.service.impl;

import org.ambraproject.rhino.AbstractRhinoTest;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate4.HibernateTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = ArticleCrudServiceImplTest.class)
@Configuration
public class ArticleCrudServiceImplTest extends AbstractRhinoTest {

  private ArticleCrudService mockArticleCrudService;

  private TaxonomyService mockTaxonomyService;

  @BeforeMethod
  public void initMocks() throws IllegalAccessException, NoSuchFieldException {
    mockArticleCrudService = applicationContext.getBean(ArticleCrudService.class);

    // MockitoAnnotations.initMocks(this);
  }

  @Bean
  public TaxonomyService taxonomyService() {
    mockTaxonomyService = mock(TaxonomyService.class);
    LOG.debug("taxonomyService() * --> {}", mockTaxonomyService);
    return mockTaxonomyService;
  }

  @Bean
  public ArticleCrudService articleCrudService() {
    mockArticleCrudService = mock(ArticleCrudService.class);
    LOG.debug("articleCrudService() * --> {}", mockArticleCrudService);
    return mockArticleCrudService;
  }
  
  @Test
  @DirtiesContext
  public void testPopulateCategories() throws Exception {
    final ArticleIdentifier articleId = ArticleIdentifier.create(Doi.create("0"));
    mockArticleCrudService.populateCategories(articleId);
    final Article article = new Article();
    article.setArticleId(0L);

    final HibernateTemplate mockHibernateTemplate =
        applicationContext.getBean(HibernateTemplate.class);

    when(mockHibernateTemplate.execute(any())).thenReturn(article);
    when(mockHibernateTemplate.execute(any())).thenReturn(1);
    ArticleRevision articleRevision = new ArticleRevision();
    when(mockHibernateTemplate.execute(any())).thenReturn(articleRevision);

    verify(mockHibernateTemplate).execute(any());
  }

  @Test
  public void testGetManuscriptXml() throws Exception {
  }

  @Test
  public void testGetManuscriptXml1() throws Exception {
  }

  @Test
  public void testGetManuscriptMetadata() throws Exception {
  }

  @Test
  public void testComplainAboutXml() throws Exception {
  }

  @Test
  public void testServeMetadata() throws Exception {
  }

  @Test
  public void testServeItems() throws Exception {
  }

  @Test
  public void testBuildOverview() throws Exception {
  }

  @Test
  public void testServeOverview() throws Exception {
  }

  @Test
  public void testServeRevisions() throws Exception {
  }

  @Test
  public void testServeRevision() throws Exception {
  }

  @Test
  public void testServeAuthors() throws Exception {
  }

  @Test
  public void testServeCategories() throws Exception {
  }

  @Test
  public void testServeRawCategories() throws Exception {
  }

  @Test
  public void testGetRawCategoriesAndText() throws Exception {
  }

  @Test
  public void testGetRelationshipsFrom() throws Exception {
  }

  @Test
  public void testGetRelationshipsTo() throws Exception {
  }

  @Test
  public void testRefreshArticleRelationships() throws Exception {
  }

  @Test
  public void testRepack() throws Exception {
  }

  @Test
  public void testGetArticleItem() throws Exception {
  }

  @Test
  public void testGetAllArticleItems() throws Exception {
  }

  @Test
  public void testGetAllArticleItems1() throws Exception {
  }

  @Test
  public void testGetItemOverview() throws Exception {
  }

  @Test
  public void testGetIngestion() throws Exception {
  }

  @Test
  public void testReadIngestion() throws Exception {
  }

  @Test
  public void testGetRevision() throws Exception {
  }

  @Test
  public void testReadRevision() throws Exception {
  }

  @Test
  public void testGetLatestRevision() throws Exception {
  }

  @Test
  public void testReadLatestRevision() throws Exception {
  }

  @Test
  public void testGetArticle() throws Exception {
  }

  @Test
  public void testReadArticle() throws Exception {
  }

  @Test
  public void testGetArticlesPublishedOn() throws Exception {
  }

  @Test
  public void testGetArticlesPublishedOn1() throws Exception {
  }

  @Test
  public void testGetArticlesRevisedOn() throws Exception {
  }

  @Test
  public void testGetArticlesRevisedOn1() throws Exception {
  }

  @Test
  public void testUpdatePreprintDoi() throws Exception {
  }

}