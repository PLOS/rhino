package org.ambraproject.rhino.service.impl;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.ambraproject.rhino.AbstractRhinoTest;
import org.ambraproject.rhino.content.xml.ArticleXml;
import org.ambraproject.rhino.content.xml.ManifestXml;
import org.ambraproject.rhino.content.xml.ManifestXml.AssetTagName;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.article.ArticleCustomMetadata;
import org.ambraproject.rhino.model.article.ArticleMetadata;
import org.ambraproject.rhino.model.ingest.ArticleItemInput;
import org.ambraproject.rhino.model.ingest.ArticlePackage;
import org.ambraproject.rhino.model.ingest.ArticlePackageBuilder;
import org.ambraproject.rhino.model.ingest.IngestPackage;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.ambraproject.rhino.service.ContentRepoPersistenceService;
import org.ambraproject.rhino.service.HibernatePersistenceService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.util.Archive;
import org.hibernate.Query;
import org.plos.crepo.model.identity.RepoVersion;
import org.plos.crepo.model.input.RepoObjectInput;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Unit tests for {@link HibernatePersistenceServiceImpl}.
 */
@ContextConfiguration(classes = HibernatePersistenceServiceTest.class)
@Configuration
public class HibernatePersistenceServiceTest extends AbstractRhinoTest {

  private static final String ARTICLE_DOI_URI = "info:doi/10.1111/dupp.0000001";

  private static final String EISSN = "EISSN-0001";

  private static final String META_JOURNAL_NAME = "Meta journal name";

  private static final Integer INGESTION_NUMBER = new Integer(5);

  private Doi articleDoi;

  private Article expectedArticle;

  private Archive mockArchive;

  private ArticleXml mockArticleXml;

  private ManifestXml mockManifest;

  private ManifestXml.Asset mockManuscriptAsset;

  private ManifestXml.ManifestFile mockManifestFile;

  private ManifestXml.Asset mockManifestAsset;

  private ManifestXml.ManifestFile mockAncillaryFile;

  private ManifestXml.Representation mockRepresentation;

  private ArticlePackage expectedArticlePackage;

  private ArticleMetadata expectedArticleMetadata;

  private ArticleCustomMetadata expectedCustomMetadata;

  private IngestPackage expectedIngestPackage;

  private Optional<Journal> expectedJournal;

  /**
   * Creates an instance of <code>HibernatePersistenceServiceTest</code>.
   */
  public HibernatePersistenceServiceTest() {
    super(true /* spyOnHibernateTemplate */);
  }

  /**
   * Initialize test data fixtures.
   */
  @Before
  public void init() {
    articleDoi = Doi.create(ARTICLE_DOI_URI);

    final LocalDate publishedOn = LocalDate.now();

    expectedArticle = new Article();
    expectedArticle.setDoi(articleDoi.getName());

    mockArchive = IngestionServiceTest.createStubArchive(new byte[] {} /* manifestXml */,
        IngestionServiceTest.ARTICLE_INGEST_ENTRIES);

    mockArticleXml = mock(ArticleXml.class);
    mockManifestFile = mock(ManifestXml.ManifestFile.class);
    mockManifestAsset = mock(ManifestXml.Asset.class);
    mockAncillaryFile = mock(ManifestXml.ManifestFile.class);
    mockRepresentation = mock(ManifestXml.Representation.class);
    mockManuscriptAsset = mock(ManifestXml.Asset.class);
    mockManifest = mock(ManifestXml.class);

    when(mockArticleXml.readDoi()).thenReturn(articleDoi);

    when(mockManifestFile.getCrepoKey()).thenReturn("10.1111/dupp.0000001.pdf");
    when(mockManifestFile.getEntry()).thenReturn("dupp.0000001.pdf");
    when(mockManifestFile.getMimetype()).thenReturn("application/pdf");

    when(mockManifestAsset.getAssetTagName()).thenReturn(AssetTagName.OBJECT);
    when(mockManifestAsset.getUri()).thenReturn("info:doi/10.1111/dupp.0000001");
    when(mockManifestAsset.isStrikingImage()).thenReturn(Boolean.TRUE);

    when(mockAncillaryFile.getCrepoKey()).thenReturn("10.1111/dupp.0000001.s001.png");
    when(mockAncillaryFile.getEntry()).thenReturn("dupp.0000001.s001.png");
    when(mockAncillaryFile.getMimetype()).thenReturn("image/ext");

    when(mockRepresentation.getFile()).thenReturn(mockManifestFile);

    when(mockManuscriptAsset.getRepresentation("manuscript"))
        .thenReturn(Optional.of(mockRepresentation));
    when(mockManuscriptAsset.getRepresentation("printable")).thenReturn(Optional.empty());
    when(mockManuscriptAsset.getUri()).thenReturn(ARTICLE_DOI_URI);

    when(mockManifest.getArticleAsset()).thenReturn(mockManuscriptAsset);
    when(mockManifest.getAssets()).thenReturn(ImmutableList.of());
    when(mockManifest.getAncillaryFiles()).thenReturn(ImmutableList.of(mockAncillaryFile));

    expectedArticlePackage =
        new ArticlePackageBuilder(mockArchive, mockArticleXml, mockManifest).build();

    expectedArticleMetadata = ArticleMetadata.builder().setTitle("Meta title")
        .setArticleType("MetaArticleType")
        .seteIssn(EISSN)
        .setJournalName(META_JOURNAL_NAME)
        .setPublicationDate(publishedOn).build();

    expectedCustomMetadata = ArticleCustomMetadata.builder().setPublicationStage("pubstage")
        .setRevisionDate(publishedOn).build();

    expectedIngestPackage =
        new IngestPackage(expectedArticlePackage, expectedArticleMetadata, expectedCustomMetadata);

    final Journal journal = new Journal();
    journal.setJournalKey(META_JOURNAL_NAME);
    journal.seteIssn(EISSN);
    expectedJournal = Optional.of(journal);
  }

  @Bean
  public JournalCrudService journalCrudService() {
    LOG.debug("journalCrudService() *");
    final JournalCrudService journalCrudService = mock(JournalCrudService.class);
    return journalCrudService;
  }

  @Bean
  public ContentRepoPersistenceService contentRepoPersistenceService() {
    LOG.debug("contentRepoPersistenceService() *");
    final ContentRepoPersistenceService contentRepoPersistenceService =
        spy(ContentRepoPersistenceServiceImpl.class);
    return contentRepoPersistenceService;
  }

  @Bean
  public HibernatePersistenceService hibernatePersistenceService() {
    final HibernatePersistenceService hibernatePersistenceService =
        spy(HibernatePersistenceServiceImpl.class);
    LOG.info("hibernatePersistenceService() ** " + hibernatePersistenceService);
    return hibernatePersistenceService;
  }

  /**
   * Test successful persisting of article to data source.
   */
  @Test
  @DirtiesContext
  public void testPersistArticleShouldSucceed() {
    final HibernateTemplate mockHibernateTemplate = buildMockHibernateTemplate();

    final HibernatePersistenceService mockPersistenceService =
        applicationContext.getBean(HibernatePersistenceService.class);
    final Article actualArticle = mockPersistenceService.persistArticle(articleDoi);

    verify(mockHibernateTemplate).save(expectedArticle);
    assertThat(actualArticle).isEqualTo(expectedArticle);
  }

  /**
   * Test successful persisting of an existing article in the data source.
   */
  @Test
  @DirtiesContext
  public void testPersistExistingArticleShouldSucceed() {
    LOG.info("testPersistExistingArticleShouldSucceed() *");
    final HibernateTemplate mockHibernateTemplate = buildMockHibernateTemplate();
    doReturn(expectedArticle).when(mockHibernateTemplate).execute(any());

    final HibernatePersistenceService mockPersistenceService =
        applicationContext.getBean(HibernatePersistenceService.class);
    final Article actualArticle = mockPersistenceService.persistArticle(articleDoi);

    verify(mockHibernateTemplate).execute(any());
    verify(mockHibernateTemplate, times(0)).save(expectedArticle);
    assertThat(actualArticle).isEqualTo(expectedArticle);
  }

  /**
   * Test successful package ingest.
   */
  @Test
  @DirtiesContext
  public void testPersistIngestionShouldSucceed() {
    final Query mockQuery = mock(Query.class);
    when(mockQuery.uniqueResult()).thenReturn(INGESTION_NUMBER);

    final HibernateTemplate mockHibernateTemplate = buildMockHibernateTemplate(mockQuery);

    final ConfigurationReadService mockConfigurationReadService =
        applicationContext.getBean(ConfigurationReadService.class);

    final JournalCrudService mockJournalCrudService =
        applicationContext.getBean(JournalCrudService.class);
    when(mockJournalCrudService.getJournalByEissn(EISSN)).thenReturn(expectedJournal);

    final HibernatePersistenceService mockPersistenceService =
        applicationContext.getBean(HibernatePersistenceService.class);

    final ArticleIngestion expectedIngestion = new ArticleIngestion();
    expectedIngestion.setArticle(expectedArticle);
    expectedIngestion.setIngestionNumber(INGESTION_NUMBER + 1);

    final ArticleIngestion actualIngestion =
        mockPersistenceService.persistIngestion(expectedArticle, expectedIngestPackage);

    assertThat(actualIngestion).isEqualTo(expectedIngestion);
    verify(mockJournalCrudService, times(1)).getJournalByEissn(EISSN);
    verify(mockHibernateTemplate).save(expectedIngestion);
  }

  /**
   * Test successful package ingest, using journal <b>Eissn</b>.
   */
  @Test
  @DirtiesContext
  public void testPersistIngestionUsingJournalEissnShouldSucceed() {
    final HibernateTemplate mockHibernateTemplate = buildMockHibernateTemplate();
    doReturn(INGESTION_NUMBER).when(mockHibernateTemplate).execute(any());

    final ConfigurationReadService mockConfigurationReadService =
        applicationContext.getBean(ConfigurationReadService.class);

    final JournalCrudService mockJournalCrudService =
        applicationContext.getBean(JournalCrudService.class);
    when(mockJournalCrudService.getJournalByEissn(EISSN)).thenReturn(expectedJournal);

    final HibernatePersistenceService mockPersistenceService =
        applicationContext.getBean(HibernatePersistenceService.class);

    final ArticleIngestion expectedIngestion = new ArticleIngestion();
    expectedIngestion.setArticle(expectedArticle);
    expectedIngestion.setIngestionNumber(INGESTION_NUMBER);

    final ArticleIngestion actualIngestion =
        mockPersistenceService.persistIngestion(expectedArticle, expectedIngestPackage);

    assertThat(actualIngestion).isEqualTo(expectedIngestion);
    verify(mockJournalCrudService).getJournalByEissn(EISSN);
    verify(mockHibernateTemplate).save(expectedIngestion);
  }

  /**
   * Test successful persisting of assets.
   */
  @Test
  @DirtiesContext
  public void testPersistAssetsShouldSucceed() {
    final HibernateTemplate mockHibernateTemplate = buildMockHibernateTemplate();

    final ContentRepoService mockContentRepoService =
        buildMockContentRepoService(DESTINATION_BUCKET);

    final ContentRepoPersistenceService mockContentRepoPersistenceService =
        applicationContext.getBean(ContentRepoPersistenceService.class);

    final HibernatePersistenceService mockPersistenceService =
        applicationContext.getBean(HibernatePersistenceService.class);

    final ArticleIngestion expectedIngestion = new ArticleIngestion();
    expectedIngestion.setArticle(expectedArticle);
    expectedIngestion.setIngestionNumber(INGESTION_NUMBER);
    expectedIngestion.setTitle(expectedArticleMetadata.getTitle());
    expectedIngestion
        .setPublicationDate(java.sql.Date.valueOf(expectedArticleMetadata.getPublicationDate()));
    expectedIngestion.setArticleType(expectedArticleMetadata.getArticleType());
    expectedIngestion.setJournal(expectedJournal.get());
    expectedIngestion
        .setRevisionDate(java.sql.Date.valueOf(expectedCustomMetadata.getRevisionDate()));
    expectedIngestion.setPublicationStage(expectedCustomMetadata.getPublicationStage());

    mockPersistenceService.persistAssets(expectedArticlePackage, expectedIngestion);

    final ImmutableList<ArticleItemInput> assets = expectedArticlePackage.getAllItems();
    assets.forEach(itemInput -> {
      verify(mockContentRepoPersistenceService).createItem(itemInput, expectedIngestion);
    });

    final ArticleItem expectedArticleItem = new ArticleItem();
    expectedArticleItem.setDoi(articleDoi.getName());
    expectedArticleItem.setIngestion(expectedIngestion);

    verify(mockHibernateTemplate).save(expectedArticleItem);

    verify(mockContentRepoService, times(2)).autoCreateRepoObject(any(RepoObjectInput.class));
    verify(mockContentRepoService, times(2)).getRepoObjectMetadata(any(RepoVersion.class));
  }

  /**
   * Test successful persisting of striking image.
   */
  @Test
  @DirtiesContext
  public void testPersistStrikingImageShouldSucceed() {
    final HibernateTemplate mockHibernateTemplate = buildMockHibernateTemplate();

    final ManifestXml mockManifest = mock(ManifestXml.class);
    when(mockManifest.getAssets()).thenReturn(ImmutableList.of(mockManifestAsset));

    final HibernatePersistenceService mockPersistenceService =
        applicationContext.getBean(HibernatePersistenceService.class);

    final ArticleIngestion expectedIngestion = new ArticleIngestion();
    expectedIngestion.setArticle(expectedArticle);
    expectedIngestion.setIngestionNumber(INGESTION_NUMBER);

    final ArticleItem expectedArticleItem = new ArticleItem();
    expectedArticleItem.setDoi(articleDoi.getName());
    expectedArticleItem.setIngestion(expectedIngestion);

    final ImmutableList<ArticleItem> articleItems = ImmutableList.of(expectedArticleItem);

    final Optional<ArticleItem> actualStrikingImage =
        mockPersistenceService.persistStrikingImage(expectedIngestion, articleItems, mockManifest);

    assertThat(actualStrikingImage).isPresent();
    assertThat(actualStrikingImage.get()).isEqualTo(expectedArticleItem);
    verify(mockHibernateTemplate).update(expectedIngestion);
  }
}
