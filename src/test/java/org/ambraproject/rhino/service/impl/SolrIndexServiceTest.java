package org.ambraproject.rhino.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;

import org.ambraproject.rhino.RhinoTestHelper;
import org.ambraproject.rhino.identity.ArticleFileIdentifier;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleFile;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.MessageSender;
import org.ambraproject.rhino.service.SolrIndexService;
import org.plos.crepo.model.metadata.RepoObjectMetadata;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;

/**
 * Unit tests for {@link SolrIndexServiceImpl}.
 */
@ContextConfiguration(classes = SolrIndexServiceTest.class)
@Configuration
public class SolrIndexServiceTest extends AbstractStubbingArticleTest {

  private static final String EISSN = "EISSN-0001";

  private static final String FILE_TYPE = "manuscript";

  private static final String META_JOURNAL_NAME = "Meta journal name";

  private static final String REPO_UUID = UUID.randomUUID().toString();

  private ContentRepoService mockContentRepoService;

  private RepoObjectMetadata mockRepoMetadata;

  private ArticleCrudService mockArticleCrudService;

  private MessageSender mockMessageSender;
  
  private SolrIndexService mockSolrIndexService;

  private Doi expectedDoi;

  private Article expectedArticle;

  private Journal expectedJournal;

  private ArticleIngestion expectedIngestion;

  private ArticleIdentifier expectedArticleIdentifier;

  private ArticleFile expectedArticleFile;

  private ArticleItem expectedArticleItem;

  /**
   * Prepare test fixtures.
   */
  @BeforeMethod(alwaysRun = true)
  protected void init() {
    mockContentRepoService = buildMockContentRepoService(DESTINATION_BUCKET);
    mockRepoMetadata = mock(RepoObjectMetadata.class);
    mockArticleCrudService = applicationContext.getBean(ArticleCrudService.class);
    mockMessageSender = applicationContext.getBean(MessageSender.class);
    mockSolrIndexService = applicationContext.getBean(SolrIndexService.class);

    expectedDoi = createStubArticleDoi();

    expectedArticle = createStubArticle(1L /* article_id */, expectedDoi.getName());

    expectedJournal = new Journal();
    expectedJournal.setJournalKey(META_JOURNAL_NAME);
    expectedJournal.seteIssn(EISSN);

    expectedIngestion = createStubArticleIngestion(expectedArticle, INGESTION_NUMBER);
    expectedIngestion.setArticle(expectedArticle);
    expectedIngestion.setJournal(expectedJournal);

    expectedArticleIdentifier = ArticleIdentifier.create(expectedDoi);

    expectedArticleFile = new ArticleFile();
    expectedArticleFile.setFileType(FILE_TYPE);
    expectedArticleFile.setBucketName(DESTINATION_BUCKET);
    expectedArticleFile.setCrepoKey(REPO_UUID);
    expectedArticleFile.setCrepoUuid(REPO_UUID);

    expectedArticleItem = new ArticleItem();
    expectedArticleItem.setDoi(expectedDoi.getName());
    expectedArticleItem.setIngestion(expectedIngestion);
    expectedArticleItem.setFiles(ImmutableList.of(expectedArticleFile));
  }

  @Bean
  public AssetCrudService assetCrudService() {
    AssetCrudService mockAssetCrudService = spy(AssetCrudServiceImpl.class);
    LOG.info("assetCrudService() * --> {}", mockAssetCrudService);
    return mockAssetCrudService;
  }

  @Bean
  public ArticleCrudService articleCrudService() {
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
   * Returns the data to test Solr indexing.
   */
  @DataProvider
  private Object[][] solrIndexData() {
    final Object[][] data = new Object[][] {
        {"activemq:plos.solr.article.index?transacted=false" /* destinationQueue */,
          false /* isLiteIndex */},
        {"activemq:plos.solr.article.lite-index?transacted=false" /* destinationQueue */,
         true /* isLiteIndex */}
    };
    return data;
  }

  /**
   * Test successful Solr index.
   *
   * @param destinationQueue The queue name used to send the indexing job
   * @param isLiteIndex Flag to indicate if indexing in <b>lite</b> mode
   *
   * @throws SAXException if failed to parse XML
   * @throws IOException  if failed to open file
   * @throws ParserConfigurationException if failed to parse XML
   */
  @Test(dataProvider = "solrIndexData")
  @DirtiesContext
  public void testUpdateSolrIndexShouldSucceed(String destinationQueue, boolean isLiteIndex)
      throws ParserConfigurationException, IOException, SAXException {
    final ArticleIngestionIdentifier expectedIngestionId = ArticleIngestionIdentifier.create(
        expectedDoi, expectedIngestion.getIngestionNumber());

    final ArticleFileIdentifier expectedManuscriptId = ArticleFileIdentifier.create(
        expectedIngestionId.getItemFor(), FILE_TYPE);

    final ArticleRevision expectedRevision = createStubArticleRevision(
        REVISION_ID, REVISION_NUMBER);
    expectedRevision.setIngestion(expectedIngestion);

    final String manuscriptXml = "dupp.0000001.xml";
    final URL manuscriptResource = Resources.getResource(SolrIndexServiceTest.class, manuscriptXml);
    final Document manuscript = RhinoTestHelper.loadXMLFromString(manuscriptResource);

    when(mockContentRepoService.getRepoObjectMetadata(expectedArticleFile.getCrepoVersion()))
        .thenReturn(mockRepoMetadata);

    doReturn(expectedArticle).when(mockArticleCrudService).readArticle(expectedArticleIdentifier);
    doReturn(expectedRevision).when(mockArticleCrudService).readLatestRevision(expectedArticle);
    doReturn(expectedRevision).when(mockArticleCrudService).readLatestRevision(expectedArticle);
    doReturn(expectedArticleItem).when(mockArticleCrudService)
        .getArticleItem(expectedManuscriptId.getItemIdentifier());
    doReturn(manuscript).when(mockArticleCrudService).getManuscriptXml(any(RepoObjectMetadata.class));

    doNothing().when(mockMessageSender).sendBody(destinationQueue, manuscript);

    mockSolrIndexService.updateSolrIndex(expectedArticleIdentifier, isLiteIndex);

    verify(mockMessageSender).sendBody(destinationQueue, manuscript);
  }

  /**
   * Test successful deletion of Solr index.
   */
  @Test
  @DirtiesContext
  public void testRemoveSolrIndexShouldSucceed() {
    final URI doiUri = expectedDoi.asUri(Doi.UriStyle.INFO_DOI);
    final String destinationQueue = "activemq:plos.solr.article.delete?transacted=false";
    doNothing().when(mockMessageSender).sendBody(destinationQueue, doiUri.toString());

    mockSolrIndexService.removeSolrIndex(expectedArticleIdentifier);

    verify(mockMessageSender).sendBody(destinationQueue, doiUri.toString());
  }
}
