package org.ambraproject.rhino;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.InputStream;
import java.util.UUID;

import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.config.YamlConfiguration;
import org.ambraproject.rhino.content.xml.CustomMetadataExtractor;
import org.ambraproject.rhino.content.xml.XpathReader;
import org.ambraproject.rhino.model.Article;
import org.ambraproject.rhino.service.AssetCrudService;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.ambraproject.rhino.service.HibernatePersistenceService;
import org.ambraproject.rhino.service.taxonomy.TaxonomyService;
import org.ambraproject.rhino.util.Java8TimeGsonAdapters;
import org.ambraproject.rhino.util.JsonAdapterUtil;
import org.ambraproject.rhino.view.article.ArticleIngestionView;
import org.ambraproject.rhino.view.article.ItemSetView;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.SessionFactoryImplementor;
import org.plos.crepo.model.identity.RepoVersion;
import org.plos.crepo.model.input.RepoObjectInput;
import org.plos.crepo.model.metadata.RepoObjectMetadata;
import org.plos.crepo.service.ContentRepoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.yaml.snakeyaml.Yaml;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Abstract base class for Rhino unit tests.
 */
public abstract class AbstractRhinoTest extends AbstractTestNGSpringContextTests {

  protected static final Logger LOG = LoggerFactory.getLogger(AbstractRhinoTest.class);

  protected static final String DESTINATION_BUCKET = "water_bucket";

  protected static final Joiner NO_SPACE_JOINER = Joiner.on("").skipNulls();

  public static final String TEST_RHINO_YAML = "rhino-test.yaml";

  /**
   * Flag to determine if <b>spying</b> on the
   * {@link org.springframework.orm.hibernate3.HibernateTemplate HibernateTemplate}.
   */
  private boolean spyOnHibernateTemplate;

  /**
   * Creates an instance of <code>HibernatePersistenceServiceTest</code>.
   */
  protected AbstractRhinoTest() {
  }

  /**
   * Creates an instance of <code>HibernatePersistenceServiceTest</code>.
   *
   * @param spyOnHibernateTemplate Flag to determine if spying on <code>HibernateTemplate</code>
   */
  protected AbstractRhinoTest(boolean spyOnHibernateTemplate) {
    this.spyOnHibernateTemplate = spyOnHibernateTemplate;
  }

  @Bean
  public SessionFactory sessionFactory() {
    final SessionFactory hibernateSessionFactory;
    if (spyOnHibernateTemplate) {
      LOG.info("hibernateSessionFactory() * Full mocking");
      hibernateSessionFactory = mock(
          SessionFactory.class, withSettings().extraInterfaces(SessionFactoryImplementor.class));

      final SessionFactoryImplementor factoryImplementor =
          (SessionFactoryImplementor) hibernateSessionFactory;
      when(factoryImplementor.getSqlFunctionRegistry())
          .thenReturn(new SQLFunctionRegistry(new MySQL5Dialect(), ImmutableMap.of()));
    } else {
      LOG.info("hibernateSessionFactory() * Simple mocking");
      hibernateSessionFactory = mock(SessionFactory.class);
    }
    return hibernateSessionFactory;
  }

  @Bean(autowire = Autowire.BY_TYPE)
  public HibernateTemplate hibernateTemplate() {
    final HibernateTemplate hibernateTemplate;
    if (spyOnHibernateTemplate) {
      LOG.info("hibernateTemplate() * Using SPY");
      hibernateTemplate = spy(new HibernateTemplate());
    } else {
      LOG.info("hibernateTemplate() *");
      hibernateTemplate = mock(HibernateTemplate.class);
    }
    return hibernateTemplate;
  }

  @Bean
  public ConfigurationReadService configurationReadService() {
    LOG.debug("configurationReadService() *");
    final ConfigurationReadService configurationReadService = mock(ConfigurationReadService.class);
    return configurationReadService;
  }

  @Bean(autowire = Autowire.BY_TYPE)
  public HibernatePersistenceService hibernatePersistenceService() {
    LOG.debug("hibernatePersistenceService() *");
    final HibernatePersistenceService hibernatePersistenceService =
        mock(HibernatePersistenceService.class);
    return hibernatePersistenceService;
  }

  @Bean
  public Gson entityGson() {
    LOG.debug("entityGson() *");
    final GsonBuilder builder = JsonAdapterUtil.makeGsonBuilder().setPrettyPrinting();
    Java8TimeGsonAdapters.register(builder);
    return builder.create();
  }

  @Bean
  public ContentRepoService contentRepoService() {
    LOG.debug("contentRepoService() *");
    final ContentRepoService contentRepoService = mock(ContentRepoService.class);
    return contentRepoService;
  }

  @Bean
  public RuntimeConfiguration runtimeConfiguration(Yaml yaml) throws Exception {
    try (final InputStream is =
        AbstractRhinoTest.class.getClassLoader().getResourceAsStream(TEST_RHINO_YAML)) {
      final YamlConfiguration runtimeConfiguration =
          new YamlConfiguration(yaml.loadAs(is, YamlConfiguration.Input.class));
      LOG.info("runtimeConfiguration: Loaded {}", TEST_RHINO_YAML);
      return spy(runtimeConfiguration);
    } catch (Exception exception) {
      LOG.warn("runtimeConfiguration: Caught exception: {}", exception);
    }
    return mock(YamlConfiguration.class);
  }

  @Bean
  public Yaml yaml() {
    final Yaml mockYaml = spy(new Yaml());
    return mockYaml;
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
    ArticleIngestionView.Factory mockArticleIngestionViewFactory = mock(ArticleIngestionView.Factory.class);
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

  /**
   * Method to mock a
   * {@link org.springframework.orm.hibernate3.HibernateTemplate HibernateTemplate},
   * using a simple mock of {@link org.hibernate.Query Query}.
   *
   * @return The {@link org.springframework.orm.hibernate3.HibernateTemplate HibernateTemplate}
   */
  public HibernateTemplate buildMockHibernateTemplate() {
    final HibernateTemplate hibernateTemplate = buildMockHibernateTemplate(mock(Query.class));
    return hibernateTemplate;
  }

  /**
   * Method to mock a
   * {@link org.springframework.orm.hibernate3.HibernateTemplate HibernateTemplate}.
   *
   * @param query The {@link org.hibernate.Query Query} to associate with
   *              {@link org.hibernate.classic.Session#createQuery(String) createQuery()}
   *
   * @return The {@link org.springframework.orm.hibernate3.HibernateTemplate HibernateTemplate}
   */
  public HibernateTemplate buildMockHibernateTemplate(Query query) {
    Preconditions.checkNotNull(query, "Query reference cannot be null");

    final SessionFactory sessionFactory = applicationContext.getBean(SessionFactory.class);
    if (spyOnHibernateTemplate) {
      final Session mockSession = mock(Session.class);
      when(mockSession.getFlushMode()).thenReturn(FlushMode.AUTO);
      when(mockSession.createQuery(anyString())).thenReturn(query);

      when(sessionFactory.openSession()).thenReturn(mockSession);
    }

    final HibernateTemplate hibernateTemplate =
        applicationContext.getBean(HibernateTemplate.class);
    return hibernateTemplate;
  }

  /**
   * Method to get the {@link org.plos.crepo.service.ContentRepoService ContentRepoService} from the
   * <b>application context</b>, and mock the following methods:
   *
   * <ul>
   * <li>autoCreateRepoObject</li>
   * </ul>
   *
   * @param bucketName The object bucket name
   *
   * @return The mocked {@link org.plos.crepo.service.ContentRepoService ContentRepoService}
   */
  public ContentRepoService buildMockContentRepoService(String bucketName) {
    final String key = UUID.randomUUID().toString();
    final String uuid = UUID.randomUUID().toString();
    final ContentRepoService mockContentRepoService =
        buildMockContentRepoService(bucketName, key, uuid);
    return mockContentRepoService;
  }

  /**
   * Method to get the {@link org.plos.crepo.service.ContentRepoService ContentRepoService} from the
   * <b>application context</b>, and mock the following methods:
   *
   * <ul>
   * <li>autoCreateRepoObject</li>
   * </ul>
   *
   * @param bucketName The object bucket name
   * @param fileSize The <b>file size</b> to return with <code>mockRepoMetadata.getSize()</code>
   *
   * @return The mocked {@link org.plos.crepo.service.ContentRepoService ContentRepoService}
   */
  public ContentRepoService buildMockContentRepoService(String bucketName, long fileSize) {
    final String key = UUID.randomUUID().toString();
    final String uuid = UUID.randomUUID().toString();
    final ContentRepoService mockContentRepoService =
        buildMockContentRepoService(bucketName, key, uuid, fileSize);
    return mockContentRepoService;
  }

  /**
   * Method to get the {@link org.plos.crepo.service.ContentRepoService ContentRepoService} from the
   * <b>application context</b>, and mock the following methods:
   *
   * <ul>
   * <li>autoCreateRepoObject</li>
   * <li>getRepoObjectMetadata</li>
   * </ul>
   *
   * @param bucketName The object bucket name
   * @param key The object key
   * @param uuid The version's UUID as a string
   *
   * @return The mocked {@link org.plos.crepo.service.ContentRepoService ContentRepoService}
   */
  public ContentRepoService buildMockContentRepoService(String bucketName, String key,
      String uuid) {
    final ContentRepoService mockContentRepoService =
        buildMockContentRepoService(bucketName, key, uuid, 5L /* fileSize */);
    return mockContentRepoService;
  }

  /**
   * Method to get the {@link org.plos.crepo.service.ContentRepoService ContentRepoService} from the
   * <b>application context</b>, and mock the following methods:
   *
   * <ul>
   * <li>autoCreateRepoObject</li>
   * <li>getRepoObjectMetadata</li>
   * </ul>
   *
   * @param bucketName The object bucket name
   * @param key The object key
   * @param uuid The version's UUID as a string
   * @param fileSize The <b>file size</b> to return with <code>mockRepoMetadata.getSize()</code>
   *
   * @return The mocked {@link org.plos.crepo.service.ContentRepoService ContentRepoService}
   */
  public ContentRepoService buildMockContentRepoService(String bucketName, String key,
      String uuid, long fileSize) {
    final ContentRepoService mockContentRepoService =
        applicationContext.getBean(ContentRepoService.class);

    final RepoVersion repoVersion = RepoVersion.create(bucketName, key, uuid);
    final RepoObjectMetadata mockRepoMetadata = mock(RepoObjectMetadata.class);
    when(mockRepoMetadata.getVersion()).thenReturn(repoVersion);
    when(mockRepoMetadata.getSize()).thenReturn(Long.valueOf(fileSize));

    when(mockContentRepoService.autoCreateRepoObject(any(RepoObjectInput.class)))
        .thenReturn(mockRepoMetadata);
    when(mockContentRepoService.getRepoObjectMetadata(any(RepoVersion.class)))
        .thenReturn(mockRepoMetadata);

    return mockContentRepoService;
  }

  /**
   * @return a stub Article object
   */
  public Article createStubArticle() {
    final Article article = new Article();
    article.setArticleId(0L);
    article.setDoi("10.1371/journal.pbio.2001414");
    return article;
  }
}
