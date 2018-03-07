package org.ambraproject.rhino.service.impl;

import org.ambraproject.rhino.AbstractRhinoTest;
import org.ambraproject.rhino.content.xml.CustomMetadataExtractor;
import org.ambraproject.rhino.content.xml.XpathReader;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.ambraproject.rhino.view.article.ArticleIngestionView;
import org.ambraproject.rhino.view.article.ItemSetView;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Class to support unit tests that require <b>article</b> samplers.
 */
public abstract class AbstractStubbingArticleTest extends AbstractRhinoTest {

  protected static final String ARTICLE_DOI = "10.1371/journal.pbio.2001414";

  protected static final String ARTICLE_DOI_URI = "info:doi/10.1111/dupp.0000001";

  protected static final Long ARTICLE_ID = 0L;

  protected static final Integer INGESTION_NUMBER = new Integer(5);

  /**
   * Returns a stubbed article Doi.
   */
  public static final Doi createStubArticleDoi() {
    final Doi articleDoi = Doi.create(ARTICLE_DOI_URI);
    return articleDoi;
  }

  /**
   * Returns a stubbed article ingestion identifier.
   */
  public static final ArticleIngestionIdentifier createStubArticleIngestionIdentifier(
      Doi articleIdentifier, int ingestionNumber) {
    final ArticleIngestionIdentifier ingestionIdentifier = ArticleIngestionIdentifier.create(
        articleIdentifier, ingestionNumber);
    return ingestionIdentifier;
  }

  public static final ArticleRevisionIdentifier createStubArticleRevisionIdentifier(
      Doi articleDoi, int revision) {
    final ArticleRevisionIdentifier revisionIdentifier =
        ArticleRevisionIdentifier.create(articleDoi, revision);
    return revisionIdentifier;
  }

  /**
   * Returns a stubbed article ingestion.
   */
  public static final ArticleIngestion createStubArticleIngestion(
      Article article, int ingestionNumber) {
    final ArticleIngestion ingestion = new ArticleIngestion();
    ingestion.setArticle(article);
    ingestion.setIngestionNumber(ingestionNumber);
    return ingestion;
  }

  /**
   * Returns a stubbed article ingestion.
   */
  public static final ArticleIngestion createStubArticleIngestion(Article article,
                                                                  int ingestionNumber, String title, LocalDate publication, String articleType, Journal journal,
                                                                  LocalDate revisionDate, String publicationStage) {
    final ArticleIngestion ingestion = createStubArticleIngestion(article, ingestionNumber);
    ingestion.setTitle(title);
    ingestion.setPublicationDate(java.sql.Date.valueOf(publication));
    ingestion.setArticleType(articleType);
    ingestion.setJournal(journal);
    ingestion.setRevisionDate(java.sql.Date.valueOf(revisionDate));
    ingestion.setPublicationStage(publicationStage);
    return ingestion;
  }

  protected static final ArticleRevision createStubArticleRevision() {
    return createStubArticleRevision(0L, 0);
  }

  /**
   * Returns a stubbed article revision.
   */
  protected static final ArticleRevision createStubArticleRevision(
      long revisionId, int revisionNumber) {
    final ArticleRevision revision = new ArticleRevision();
    revision.setRevisionId(revisionId);
    revision.setRevisionNumber(revisionNumber);
    revision.setIngestion(createStubArticleIngestion(createStubArticle(), 1));
    return revision;
  }

  /**
   * @return a stub Article object
   */
  public static final Article createStubArticle() {
    final Article article = new Article();
    article.setArticleId(ARTICLE_ID);
    article.setDoi(ARTICLE_DOI);
    return article;
  }

  /**
   * Creates an instance of <code>AbstractStubbingArticleTest</code>.
   */
  public AbstractStubbingArticleTest() {
    super();
  }

  /**
   * Creates an instance of <code>AbstractStubbingArticleTest</code>.
   *
   * @param spyOnHibernateTemplate Flag to determine if spying on <code>HibernateTemplate</code>
   */
  public AbstractStubbingArticleTest(boolean spyOnHibernateTemplate) {
    super(spyOnHibernateTemplate);
  }

  @Bean
  public AssetCrudService assetCrudService() {
    AssetCrudService mockAssetCrudService = mock(AssetCrudService.class);
    LOG.debug("assetCrudService() * --> {}", mockAssetCrudService);
    return mockAssetCrudService;
  }

  @Bean
  public XpathReader xpathReader() {
    XpathReader mockXpathReader = mock(XpathReader.class);
    LOG.debug("xpathReader() * --> {}", mockXpathReader);
    return mockXpathReader;
  }

  @Bean
  public ArticleIngestionView.Factory articleIngestionViewFactory() {
    ArticleIngestionView.Factory mockArticleIngestionViewFactory =
        mock(ArticleIngestionView.Factory.class);
    LOG.debug("articleIngestionView.Factory() * --> {}", mockArticleIngestionViewFactory);
    return mockArticleIngestionViewFactory;
  }

  @Bean
  public ItemSetView.Factory itemSetViewFactory() {
    ItemSetView.Factory mockItemSetViewFactory = mock(ItemSetView.Factory.class);
    LOG.debug("itemSetViewFactory() * --> {}", mockItemSetViewFactory);
    return mockItemSetViewFactory;
  }

  @Bean
  public CustomMetadataExtractor.Factory customMetadataExtractorFactory() {
    CustomMetadataExtractor.Factory mockMetadataExtractorFactory =
        spy(CustomMetadataExtractor.Factory.class);
    LOG.debug("customMetadataExtractorFactory() * {}", mockMetadataExtractorFactory);
    return mockMetadataExtractorFactory;
  }

  @Bean
  public TaxonomyService taxonomyService() {
    TaxonomyService mockTaxonomyService = mock(TaxonomyService.class);
    LOG.debug("taxonomyService() * --> {}", mockTaxonomyService);
    return mockTaxonomyService;
  }

  @Bean
  public ArticleCrudService articleCrudService() {
    ArticleCrudService mockArticleCrudService = mock(ArticleCrudServiceImpl.class);
    LOG.debug("articleCrudService() * --> {}", mockArticleCrudService);
    return mockArticleCrudService;
  }

  @Bean
  public JournalCrudService journalCrudService() {
    JournalCrudService mockJournalCrudService = mock(JournalCrudService.class);
    LOG.debug("journalCrudService() * --> {}", mockJournalCrudService);
    return mockJournalCrudService;
  }
}
