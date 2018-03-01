package org.ambraproject.rhino.service.impl;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.model.article.ArticleCustomMetadata;
import org.ambraproject.rhino.model.article.ArticleMetadata;
import org.ambraproject.rhino.model.ingest.ArticlePackage;
import org.ambraproject.rhino.model.ingest.ArticlePackageBuilder;
import org.ambraproject.rhino.model.ingest.IngestPackage;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.ambraproject.rhino.service.ContentRepoPersistenceService;
import org.ambraproject.rhino.service.HibernatePersistenceService;
import org.ambraproject.rhino.service.JournalCrudService;
import org.ambraproject.rhino.util.Archive;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Unit tests for {@link HibernatePersistenceServiceImpl}.
 */
@ContextConfiguration(classes = HibernatePersistenceServiceTest.class)
@Configuration
public class HibernatePersistenceServiceTest extends AbstractRhinoTest {

  private static final String ARTICLE_DOI_URI = "info:doi/10.1371/journal.pbio.0040019";

  private static final String DESTINATION_BUCKET = "water_bucket";

  private static final String EISSN = "EISSN-0001";

  private static final String META_JOURNAL_NAME = "Meta journal name";

  private static final Integer NEXT_INGESTION_NUMBER = new Integer(5);

  private static final ImmutableMap<String, Object> REPO_CONFIG = ImmutableMap
      .of("corpus", ImmutableMap.of("secondaryBuckets", ImmutableSet.of(DESTINATION_BUCKET)));

  private Doi articleDoi;

  private Article expectedArticle;

  private Archive mockArchive;

  private ArticleXml mockArticleXml;

  private ManifestXml mockManifest;

  private ManifestXml.Asset mockManuscriptAsset;

  private ManifestXml.ManifestFile mockManifestFile;

  private ManifestXml.Representation mockRepresentation;

  private ArticlePackage expectedArticlePackage;

  private ArticleMetadata expectedArticleMetadata;

  private ArticleCustomMetadata expectedCustomMetadata;

  private IngestPackage expectedIngestPackage;

  private Optional<Journal> expectedJournal;

  /**
   * Initialize test data fixtures.
   */
  @BeforeMethod(alwaysRun = true)
  public void init() {
    articleDoi = Doi.create(ARTICLE_DOI_URI);

    final LocalDate publishedOn = LocalDate.now();

    expectedArticle = new Article();
    expectedArticle.setDoi(articleDoi.getName());

    mockArchive = IngestionServiceTest.createStubArchive(new byte[] {},
        IngestionServiceTest.ARTICLE_INGEST_ENTRIES);

    mockArticleXml = mock(ArticleXml.class);
    mockManifestFile = mock(ManifestXml.ManifestFile.class);
    mockRepresentation = mock(ManifestXml.Representation.class);
    mockManuscriptAsset = mock(ManifestXml.Asset.class);
    mockManifest = mock(ManifestXml.class);

    when(mockArticleXml.readDoi()).thenReturn(articleDoi);

    when(mockManifestFile.getCrepoKey()).thenReturn("10.1371/dupp.0000001.pdf");
    when(mockManifestFile.getEntry()).thenReturn("dupp.0000001.pdf");
    when(mockManifestFile.getMimetype()).thenReturn("application/pdf");

    when(mockRepresentation.getFile()).thenReturn(mockManifestFile);

    when(mockManuscriptAsset.getRepresentation("manuscript"))
        .thenReturn(Optional.of(mockRepresentation));
    when(mockManuscriptAsset.getRepresentation("printable")).thenReturn(Optional.empty());
    when(mockManuscriptAsset.getUri()).thenReturn(ARTICLE_DOI_URI);

    when(mockManifest.getArticleAsset()).thenReturn(mockManuscriptAsset);
    when(mockManifest.getAssets()).thenReturn(ImmutableList.of());
    when(mockManifest.getAncillaryFiles()).thenReturn(ImmutableList.of());

    expectedArticlePackage =
        new ArticlePackageBuilder(DESTINATION_BUCKET, mockArchive, mockArticleXml, mockManifest)
            .build();

    expectedArticleMetadata = ArticleMetadata.builder().setTitle("Meta title")
        .setArticleType("MetaArticleType")
        .setEissn(EISSN)
        .setJournalName(META_JOURNAL_NAME)
        .setPublicationDate(publishedOn).build();

    expectedCustomMetadata = ArticleCustomMetadata.builder().setPublicationStage("pubtage")
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
        mock(ContentRepoPersistenceService.class);
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
    final HibernateTemplate mockHibernateTemplate =
        applicationContext.getBean(HibernateTemplate.class);

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
    final HibernateTemplate mockHibernateTemplate =
        applicationContext.getBean(HibernateTemplate.class);
    when(mockHibernateTemplate.execute(any())).thenReturn(expectedArticle);

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
    final HibernateTemplate mockHibernateTemplate =
        applicationContext.getBean(HibernateTemplate.class);
    when(mockHibernateTemplate.execute(any())).thenReturn(NEXT_INGESTION_NUMBER);

    final ConfigurationReadService mockConfigurationReadService =
        applicationContext.getBean(ConfigurationReadService.class);
    when(mockConfigurationReadService.getRepoConfig()).thenReturn(REPO_CONFIG);

    final JournalCrudService mockJournalCrudService =
        applicationContext.getBean(JournalCrudService.class);
    when(mockJournalCrudService.getJournal(META_JOURNAL_NAME)).thenReturn(expectedJournal);

    final HibernatePersistenceService mockPersistenceService =
        applicationContext.getBean(HibernatePersistenceService.class);

    final ArticleIngestion expectedIngestion = new ArticleIngestion();
    expectedIngestion.setArticle(expectedArticle);
    expectedIngestion.setIngestionNumber(NEXT_INGESTION_NUMBER);

    final ArticleIngestion actualIngestion =
        mockPersistenceService.persistIngestion(expectedArticle, expectedIngestPackage);

    assertThat(actualIngestion).isEqualTo(expectedIngestion);
    verify(mockConfigurationReadService).getRepoConfig();
    verify(mockJournalCrudService, times(0)).getJournalByEissn(EISSN);
    verify(mockJournalCrudService).getJournal(META_JOURNAL_NAME);
    verify(mockHibernateTemplate).save(expectedIngestion);
  }

  /**
   * Test successful package ingest, using journal <b>Eissn</b>.
   */
  @Test
  @DirtiesContext
  public void testPersistIngestionUsingJournalEissnShouldSucceed() {
    final HibernateTemplate mockHibernateTemplate =
        applicationContext.getBean(HibernateTemplate.class);
    when(mockHibernateTemplate.execute(any())).thenReturn(NEXT_INGESTION_NUMBER);

    final ImmutableMap<String, Object> repoConfig =
        ImmutableMap.of("corpus", ImmutableMap.of("secondaryBuckets", ImmutableSet.of()));

    final ConfigurationReadService mockConfigurationReadService =
        applicationContext.getBean(ConfigurationReadService.class);
    when(mockConfigurationReadService.getRepoConfig()).thenReturn(repoConfig);

    final JournalCrudService mockJournalCrudService =
        applicationContext.getBean(JournalCrudService.class);
    when(mockJournalCrudService.getJournalByEissn(EISSN)).thenReturn(expectedJournal);

    final HibernatePersistenceService mockPersistenceService =
        applicationContext.getBean(HibernatePersistenceService.class);

    final ArticleIngestion expectedIngestion = new ArticleIngestion();
    expectedIngestion.setArticle(expectedArticle);
    expectedIngestion.setIngestionNumber(NEXT_INGESTION_NUMBER);

    final ArticleIngestion actualIngestion =
        mockPersistenceService.persistIngestion(expectedArticle, expectedIngestPackage);

    assertThat(actualIngestion).isEqualTo(expectedIngestion);
    verify(mockConfigurationReadService).getRepoConfig();
    verify(mockJournalCrudService).getJournalByEissn(EISSN);
    verify(mockJournalCrudService, times(0)).getJournal(META_JOURNAL_NAME);
    verify(mockHibernateTemplate).save(expectedIngestion);
  }

  /**
   * Test successful persisting of assets.
   */
  @DirtiesContext
  public void testPersistAssetsShouldSucceed() {
    final HibernateTemplate mockHibernateTemplate =
        applicationContext.getBean(HibernateTemplate.class);
    when(mockHibernateTemplate.execute(any())).thenReturn(NEXT_INGESTION_NUMBER);

    final ConfigurationReadService mockConfigurationReadService =
        applicationContext.getBean(ConfigurationReadService.class);
    when(mockConfigurationReadService.getRepoConfig()).thenReturn(REPO_CONFIG);

    final JournalCrudService mockJournalCrudService =
        applicationContext.getBean(JournalCrudService.class);
    when(mockJournalCrudService.getJournal(META_JOURNAL_NAME)).thenReturn(expectedJournal);

    final HibernatePersistenceService mockPersistenceService =
        applicationContext.getBean(HibernatePersistenceService.class);

    final ArticleIngestion expectedIngestion = new ArticleIngestion();
    expectedIngestion.setArticle(expectedArticle);
    expectedIngestion.setIngestionNumber(NEXT_INGESTION_NUMBER);
    expectedIngestion.setTitle(expectedArticleMetadata.getTitle());
    expectedIngestion
        .setPublicationDate(java.sql.Date.valueOf(expectedArticleMetadata.getPublicationDate()));
    expectedIngestion.setArticleType(expectedArticleMetadata.getArticleType());
    expectedIngestion.setJournal(expectedJournal.get());
    expectedIngestion
        .setRevisionDate(java.sql.Date.valueOf(expectedCustomMetadata.getRevisionDate()));
    expectedIngestion.setPublicationStage(expectedCustomMetadata.getPublicationStage());

    final ArticleIngestion actualIngestion =
        mockPersistenceService.persistIngestion(expectedArticle, expectedIngestPackage);

    assertThat(actualIngestion).isEqualTo(expectedIngestion);
    verify(mockConfigurationReadService).getRepoConfig();
    verify(mockJournalCrudService).getJournal(META_JOURNAL_NAME);
    verify(mockHibernateTemplate).save(expectedIngestion);
  }
}