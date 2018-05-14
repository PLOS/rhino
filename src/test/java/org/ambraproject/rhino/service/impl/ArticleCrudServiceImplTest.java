package org.ambraproject.rhino.service.impl;

import com.google.common.collect.ImmutableList;
import org.ambraproject.rhino.identity.ArticleIdentifier;
import org.ambraproject.rhino.identity.ArticleIngestionIdentifier;
import org.ambraproject.rhino.identity.ArticleItemIdentifier;
import org.ambraproject.rhino.identity.ArticleRevisionIdentifier;
import org.ambraproject.rhino.identity.Doi;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.model.ArticleFile;
import org.ambraproject.rhino.model.ArticleIngestion;
import org.ambraproject.rhino.model.ArticleItem;
import org.ambraproject.rhino.model.ArticleRelationship;
import org.ambraproject.rhino.model.ArticleRevision;
import org.ambraproject.rhino.model.Journal;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleCrudService.SortOrder;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.ambraproject.rhino.view.ResolvedDoiView;
import org.ambraproject.rhino.view.article.ArticleOverview;
import org.hibernate.criterion.DetachedCriteria;
import org.plos.crepo.model.identity.RepoVersion;
import org.plos.crepo.model.metadata.RepoObjectMetadata;
import org.plos.crepo.service.ContentRepoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = ArticleCrudServiceImplTest.class)
@Configuration
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class ArticleCrudServiceImplTest extends AbstractStubbingArticleTest {

  private ArticleCrudService mockArticleCrudService;

  private HibernateTemplate mockHibernateTemplate;

  private Article stubArticle = createStubArticle();

  private final ArticleIdentifier stubArticleId = ArticleIdentifier.create(Doi.create("0"));

  @BeforeMethod(alwaysRun = true)
  public void initMocks() throws IllegalAccessException, NoSuchFieldException {
    mockArticleCrudService = applicationContext.getBean(ArticleCrudService.class);
    mockHibernateTemplate = applicationContext.getBean(HibernateTemplate.class);
  }

  @Bean
  public ArticleCrudService articleCrudService() {
    mockArticleCrudService = spy(ArticleCrudServiceImpl.class);
    LOG.debug("articleCrudService() * --> {}", mockArticleCrudService);
    return mockArticleCrudService;
  }

  private Collection<ArticleRevision> createStubArticleRevisions() {
    final ArticleRevision dummyRevision = createStubArticleRevision();
    Collection<ArticleRevision> dummyRevisions = ImmutableList.of(dummyRevision);
    when(mockHibernateTemplate.execute(any())).thenReturn(dummyRevisions);
    return dummyRevisions;
  }

  private InputStream createStubInputStream() throws TransformerException {
    String xmlString = "<?xml version=\"1.0\" encoding=\"utf-8\"?><a><b></b><c></c></a>";

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    Document document = null;
    try {
      builder = factory.newDocumentBuilder();
      document = builder.parse(new InputSource(new StringReader(xmlString)));
    } catch (Exception e) {
      e.printStackTrace();
    }

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Source xmlSource = new DOMSource(document);
    Result outputTarget = new StreamResult(outputStream);
    TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
    return new ByteArrayInputStream(outputStream.toByteArray());
  }

  private ArticleRevision setCommonArticleMocks() {
    ArticleRevision articleRevision = createStubArticleRevision();
    when(mockHibernateTemplate.execute(any())).thenReturn(stubArticle).thenReturn(1)
        .thenReturn(articleRevision);
    return articleRevision;
  }

  @Test
  @DirtiesContext
  public void testPopulateCategories() throws Exception {

    ArticleRevision articleRevision = setCommonArticleMocks();

    mockArticleCrudService.populateCategories(stubArticleId);

    TaxonomyService mockTaxonomyService = applicationContext.getBean(TaxonomyService.class);
    verify(mockTaxonomyService).populateCategories(articleRevision);
  }

  @Test
  public void testGetManuscriptXml() throws Exception {
    setCommonManuscriptMocks();

    ArticleIngestion articleIngestion = new ArticleIngestion();
    articleIngestion.setArticle(createStubArticle());
    mockArticleCrudService.getManuscriptXml(articleIngestion);
  }

  private void setCommonManuscriptMocks() throws TransformerException {
    final AssetCrudService mockAssetCrudService = applicationContext.getBean(AssetCrudService.class);
    final ContentRepoService mockContentRepoService = applicationContext.getBean(ContentRepoService.class);

    final HashMap<String, Object> repoObjectMap = new HashMap<>();
    repoObjectMap.put("key", "0");
    repoObjectMap.put("uuid", "50a25f86-bcf2-4577-a3a1-e4f673615de8");
    final RepoObjectMetadata repoObjectMetadata
        = new RepoObjectMetadata("bucket", repoObjectMap);

    when(mockAssetCrudService.getArticleItemFile(any())).thenReturn(repoObjectMetadata);

    when(mockContentRepoService.getRepoObject(any(RepoVersion.class)))
        .thenReturn(createStubInputStream());
  }

  @Test
  public void testBuildOverview() throws Exception {
    final ArticleOverview articleOverview = ArticleOverview.build(stubArticleId, ImmutableList.of(),
        ImmutableList.of());
    when(mockHibernateTemplate.execute(any())).thenReturn(articleOverview);
    final ArticleOverview returnedArticleOverview
        = mockArticleCrudService.buildOverview(createStubArticle());
    assertThat(returnedArticleOverview).isEqualTo(articleOverview);
  }

  @Test
  public void testGetRawCategoriesAndText() throws Exception {
    setCommonArticleMocks();
    setCommonManuscriptMocks();

    final ArrayList<String> categoryList = new ArrayList<>();
    categoryList.add("test/test");

    TaxonomyService mockTaxonomyService = applicationContext.getBean(TaxonomyService.class);
    when(mockTaxonomyService.getRawTerms(any(Document.class), eq(stubArticle), eq(true)))
        .thenReturn(categoryList);

    final String rawCategoriesAndText = mockArticleCrudService.getRawCategoriesAndText(stubArticleId);
    assertThat(rawCategoriesAndText).isEqualTo("<pre>test/test\n</pre>");
  }

  @Test
  public void testGetRelationshipsFrom() throws Exception {
    final List<ArticleRelationship> expectedRelationships = ImmutableList.of();
    when(mockHibernateTemplate.execute(any())).thenReturn(expectedRelationships);

    final List<ArticleRelationship> returnedRelationships
        = mockArticleCrudService.getRelationshipsFrom(stubArticleId);

    assertThat(expectedRelationships).isEqualTo(returnedRelationships);
  }

  @Test
  public void testGetRelationshipsTo() throws Exception {
    final List<ArticleRelationship> expectedRelationships = ImmutableList.of();
    when(mockHibernateTemplate.execute(any())).thenReturn(expectedRelationships);

    final List<ArticleRelationship> returnedRelationships
        = mockArticleCrudService.getRelationshipsTo(stubArticleId);

    assertThat(expectedRelationships).isEqualTo(returnedRelationships);
  }

  @Test
  public void testRefreshArticleRelationships() throws Exception {
    setCommonManuscriptMocks();

    final ArticleRelationship dummyRelationship = new ArticleRelationship();
    final List<ArticleRelationship> dummyRelationships = ImmutableList.of(dummyRelationship);
    when(mockHibernateTemplate.execute(any())).thenReturn(dummyRelationships);

    mockArticleCrudService.refreshArticleRelationships(createStubArticleRevision());

    verify(mockHibernateTemplate).delete(dummyRelationship);
  }

  @Test
  public void testRepack() throws Exception {
    final ArticleIngestionIdentifier dummyIngestionId
        = ArticleIngestionIdentifier.create("journal.pbio.2001414", 1);

    final ContentRepoService mockContentRepoService
        = buildMockContentRepoService("bucket");

    final ArticleFile dummyFile = new ArticleFile();
    dummyFile.setCrepoKey("test");
    dummyFile.setBucketName("bucket");
    dummyFile.setCrepoUuid("50a25f86-bcf2-4577-a3a1-e4f673615de8");
    dummyFile.setIngestedFileName("test");
    List<ArticleFile> dummyFiles = ImmutableList.of(dummyFile);

    when(mockHibernateTemplate.execute(any())).thenReturn(createStubArticleRevision().getIngestion())
        .thenReturn(dummyFiles);

    when(mockContentRepoService.getRepoObject(any(RepoVersion.class))).thenReturn(createStubInputStream());
    mockArticleCrudService.repack(dummyIngestionId);
  }

  @Test
  public void testGetArticleItem() throws Exception {
    final ArticleItem dummyItem = new ArticleItem();
    when(mockHibernateTemplate.execute(any())).thenReturn(dummyItem);

    final ArticleItem mockItem = mockArticleCrudService
        .getArticleItem(ArticleItemIdentifier.create("0", 1));
    assertThat(mockItem).isEqualTo(dummyItem);
  }

  @Test
  public void testGetAllArticleItemsByDoi() throws Exception {
    final List<ArticleItem> expectedItems = ImmutableList.of(new ArticleItem());
    when(mockHibernateTemplate.execute(any())).thenReturn(expectedItems);

    final Collection<ArticleItem> returnedItems
        = mockArticleCrudService.getAllArticleItems(Doi.create("test"));

    assertThat(expectedItems).isEqualTo(returnedItems);
  }

  @Test
  public void testGetAllArticleItemsByIngestion() throws Exception {
    final List<ArticleItem> expectedItems = ImmutableList.of(new ArticleItem());
    when(mockHibernateTemplate.execute(any())).thenReturn(expectedItems);

    final Collection<ArticleItem> returnedItems
        = mockArticleCrudService.getAllArticleItems(createStubArticleRevision().getIngestion());

    assertThat(expectedItems).isEqualTo(returnedItems);
  }

  @Test
  public void testGetItemOverview() throws Exception {
    final Doi dummyDoi = Doi.create("0");
    ResolvedDoiView dummyView = ResolvedDoiView
        .create(dummyDoi, ResolvedDoiView.DoiWorkType.ISSUE, new Journal());
    when(mockHibernateTemplate.execute(any())).thenReturn(Optional.of(dummyView));

    final Optional<ResolvedDoiView> itemOverview = mockArticleCrudService.getItemOverview(dummyDoi);
    assertThat(dummyView).isEqualTo(itemOverview.get());
  }

  @Test
  public void testReadIngestion() throws Exception {
    final ArticleIngestion dummyIngestion = createStubArticleRevision().getIngestion();
    when(mockHibernateTemplate.execute(any())).thenReturn(dummyIngestion);

    final ArticleIngestion mockIngestion = mockArticleCrudService
        .readIngestion(ArticleIngestionIdentifier.create("0", 1));
    assertThat(mockIngestion).isEqualTo(dummyIngestion);
  }

  @Test
  public void testReadRevision() throws Exception {
    final ArticleRevision dummyRevision = createStubArticleRevision();
    when(mockHibernateTemplate.execute(any())).thenReturn(dummyRevision);

    final ArticleRevision mockRevision = mockArticleCrudService
        .readRevision(ArticleRevisionIdentifier.create("0", 1));
    assertThat(mockRevision).isEqualTo(dummyRevision);
  }

  @Test
  public void testReadLatestRevision() throws Exception {
    final ArticleRevision stubArticleRevision = createStubArticleRevision();
    when(mockHibernateTemplate.execute(any())).thenReturn(1)
        .thenReturn(stubArticleRevision);
    final ArticleRevision mockRevision = mockArticleCrudService
        .readLatestRevision(stubArticle);
    assertThat(mockRevision).isEqualTo(stubArticleRevision);
  }

  @Test
  public void testReadArticle() throws Exception {
    when(mockHibernateTemplate.execute(any())).thenReturn(stubArticle);

    final Article mockArticle = mockArticleCrudService.readArticle(ArticleIdentifier.create("0"));
    assertThat(mockArticle).isEqualTo(stubArticle);
  }

  @Test
  public void testGetArticlesPublishedOn() throws Exception {
    Collection<ArticleRevision> dummyRevisions = createStubArticleRevisions();

    final Collection<ArticleRevision> mockArticleRevisions
        = mockArticleCrudService.getArticlesPublishedOn(LocalDate.now(), LocalDate.now());
    assertThat(mockArticleRevisions).isEqualTo(dummyRevisions);
  }

  @Test
  public void testGetArticlesRevisedOn() throws Exception {
    Collection<ArticleRevision> dummyRevisions = createStubArticleRevisions();

    final Collection<ArticleRevision> mockArticleRevisions
        = mockArticleCrudService.getArticlesRevisedOn(LocalDate.now(), LocalDate.now());
    assertThat(mockArticleRevisions).isEqualTo(dummyRevisions);
  }

  @Test
  public void testUpdatePreprintDoi() throws Exception {

    final ArticleIngestion stubIngestion = createStubArticleRevision().getIngestion();
    when(mockHibernateTemplate.execute(any())).thenReturn(stubIngestion);

    final ArticleIngestionIdentifier articleId = ArticleIngestionIdentifier.create("0", 1);
    mockArticleCrudService.updatePreprintDoi(articleId, "preprintDoi");

    verify(mockHibernateTemplate).save(stubIngestion);
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  /**
   * Test getting list of article DOIs.
   */
  public void testGetListOfArticleDOIsShouldSucceed() {
    final long articlesCount = 20;
    final List expectedDois = ImmutableList.of(
        "10.1371/journal.ppat.1006521",
        "10.1371/journal.pone.0180908",
        "10.1371/journal.pgen.1006865",
        "10.1371/journal.pone.0189616",
        "10.1371/journal.pone.0187101");

    when(mockHibernateTemplate.execute(any())).thenReturn(articlesCount);
    when(mockHibernateTemplate.findByCriteria(
        any(DetachedCriteria.class), anyInt(), anyInt())).thenReturn(expectedDois);

    Collection<String> actualDois = mockArticleCrudService.getArticleDois(
        1 /* pageNumber */, 5 /* pageSize */, SortOrder.NEWEST);
    assertThat(actualDois).isEqualTo(expectedDois);

    verify(mockHibernateTemplate).execute(any());
    verify(mockHibernateTemplate).findByCriteria(any(DetachedCriteria.class), anyInt(), anyInt());

    // Test when "pageNumber" results in an query offset greater than the number of records. 
    actualDois = mockArticleCrudService.getArticleDois(
        5 /* pageNumber */, 5 /* pageSize */, SortOrder.NEWEST);
    assertThat(actualDois).isEmpty();
  }

  @DataProvider
  protected Object [][] dateRanges() {
    return new Object[][] {
        {LocalDateTime.now(), LocalDateTime.now()},
        {LocalDateTime.now(), null},
        {null, LocalDateTime.now()}
    };
  }

  @Test(dataProvider = "dateRanges")
  @SuppressWarnings({"rawtypes", "unchecked"})
  /**
   * Test getting list of article DOIs with a specified date range.
   */
  public void testGetListOfArticleDOIsWithDateRangeShouldSucceed(
      LocalDateTime starting, LocalDateTime ending) {
    final long articlesCount = 20;
    final Optional<LocalDateTime> fromDate = Optional.ofNullable(starting);
    final Optional<LocalDateTime> toDate = Optional.ofNullable(ending);
    final List expectedDois = ImmutableList.of(
        "10.1371/journal.ppat.1006521",
        "10.1371/journal.pone.0180908",
        "10.1371/journal.pgen.1006865",
        "10.1371/journal.pone.0189616",
        "10.1371/journal.pone.0187101");

    when(mockHibernateTemplate.execute(any())).thenReturn(articlesCount);
    when(mockHibernateTemplate.findByCriteria(
        any(DetachedCriteria.class), anyInt(), anyInt())).thenReturn(expectedDois);

    final Collection<String> actualDois = mockArticleCrudService.getArticleDoisForDateRange(
        0, 5, SortOrder.OLDEST, fromDate, toDate);
    assertThat(actualDois).isEqualTo(expectedDois);
    verify(mockHibernateTemplate).execute(any());
    verify(mockHibernateTemplate).findByCriteria(any(DetachedCriteria.class), anyInt(), anyInt());
  }
}
