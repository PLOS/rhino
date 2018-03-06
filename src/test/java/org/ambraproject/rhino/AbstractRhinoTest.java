package org.ambraproject.rhino;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.config.YamlConfiguration;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.ambraproject.rhino.service.HibernatePersistenceService;
import org.ambraproject.rhino.util.Java8TimeGsonAdapters;
import org.ambraproject.rhino.util.JsonAdapterUtil;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.SessionFactoryImplementor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
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
import org.testng.annotations.BeforeMethod;
import org.yaml.snakeyaml.Yaml;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Abstract base class for Rhino unit tests.
 */
public abstract class AbstractRhinoTest extends AbstractTestNGSpringContextTests {

  protected static final Logger LOG = LoggerFactory.getLogger(AbstractRhinoTest.class);

  protected static final Joiner NO_SPACE_JOINER = Joiner.on("").skipNulls();

  public static final String TEST_RHINO_YAML = "rhino-test.yaml";

  protected String currentTestMethod;

  @BeforeMethod(alwaysRun = true)
  @Override
  protected void springTestContextBeforeTestMethod(Method testMethod) throws Exception {
    currentTestMethod = testMethod.getName();
    LOG.info("springTestContextBeforeTestMethod() * {}", currentTestMethod);
    super.springTestContextBeforeTestMethod(testMethod);
  }

  /** Returns a query. */
  protected Query createQuery(InvocationOnMock invocation) {
    final Query mockQuery = mock(Query.class);
    return mockQuery;
  }

  @Bean
  public SessionFactory sessionFactory() {
    LOG.info("hibernateSessionFactory() *");
    final SessionFactory hibernateSessionFactory = mock(
        SessionFactory.class, withSettings().extraInterfaces(SessionFactoryImplementor.class));

    final Session mockSession = mock(Session.class);
    when(mockSession.getFlushMode()).thenReturn(FlushMode.AUTO);
    when(mockSession.createQuery(anyString())).thenAnswer(new Answer<Query>() {

      @Override
      public Query answer(InvocationOnMock invocation) throws Throwable {
        return createQuery(invocation);
      }

    });

    when(hibernateSessionFactory.openSession()).thenReturn(mockSession);

    final SessionFactoryImplementor factoryImplementor =
        (SessionFactoryImplementor) hibernateSessionFactory;
    when(factoryImplementor.getSqlFunctionRegistry())
        .thenReturn(new SQLFunctionRegistry(new MySQL5Dialect(), ImmutableMap.of()));
    return hibernateSessionFactory;
  }

  @Bean(autowire = Autowire.BY_TYPE)
  public HibernateTemplate hibernateTemplate() {
    LOG.info("hibernateTemplate() *");
    final HibernateTemplate hibernateTemplate = spy(new HibernateTemplate());
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

  /**
   * Method to mock a {@link org.hibernate.SessionFactory Hibernate Session Factor}.
   *
   * @param query The {@link org.hibernate.Query Query} to associate with
   *              {@link org.hibernate.classic.Session#createQuery(String) createQuery()}
   *
   * @return The {@link org.hibernate.SessionFactory Hibernate Session Factor}
   */
  public SessionFactory buildMockHibernateSessionFactory(Optional<Query> query) {
    final SessionFactory sessionFactory = applicationContext.getBean(SessionFactory.class);

    final Session mockSession = mock(Session.class);
    when(mockSession.getFlushMode()).thenReturn(FlushMode.AUTO);

    if (query.isPresent()) {
      when(mockSession.createQuery(anyString())).thenReturn(query.get());
    } else {
      when(mockSession.createQuery(anyString())).thenReturn(mock(Query.class));
    }

    when(sessionFactory.openSession()).thenReturn(mockSession);

    return sessionFactory;
  }

  /**
   * Method to get the {@link org.plos.crepo.service.ContentRepoService ContentRepoService} from the
   * <b>application context</b>, and mock the following methods:
   *
   * <ul>
   * <li>autoCreateRepoObject</li>
   * </ul>
   *
   * @param bucketName the object bucket name
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
   * <li>getRepoObjectMetadata</li>
   * </ul>
   *
   * @param bucketName the object bucket name
   * @param key the object key
   * @param uuid the version's UUID as a string
   *
   * @return The mocked {@link org.plos.crepo.service.ContentRepoService ContentRepoService}
   */
  public ContentRepoService buildMockContentRepoService(String bucketName, String key,
      String uuid) {
    final ContentRepoService mockContentRepoService =
        applicationContext.getBean(ContentRepoService.class);

    final RepoVersion repoVersion = RepoVersion.create(bucketName, key, uuid);
    final RepoObjectMetadata mockRepoMetadata = mock(RepoObjectMetadata.class);
    when(mockRepoMetadata.getVersion()).thenReturn(repoVersion);
    when(mockRepoMetadata.getSize()).thenReturn(5L);

    when(mockContentRepoService.autoCreateRepoObject(any(RepoObjectInput.class)))
        .thenReturn(mockRepoMetadata);
    when(mockContentRepoService.getRepoObjectMetadata(any(RepoVersion.class)))
        .thenReturn(mockRepoMetadata);

    return mockContentRepoService;
  }
}
