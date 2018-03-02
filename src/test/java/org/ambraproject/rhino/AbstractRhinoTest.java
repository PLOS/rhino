package org.ambraproject.rhino;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.UUID;

import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.config.YamlConfiguration;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.ambraproject.rhino.service.HibernatePersistenceService;
import org.ambraproject.rhino.util.Java8TimeGsonAdapters;
import org.ambraproject.rhino.util.JsonAdapterUtil;
import org.plos.crepo.model.identity.RepoVersion;
import org.plos.crepo.model.input.RepoObjectInput;
import org.plos.crepo.model.metadata.RepoObjectMetadata;
import org.plos.crepo.service.ContentRepoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.yaml.snakeyaml.Yaml;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Abstract base class for Rhino unit tests.
 */
public abstract class AbstractRhinoTest extends AbstractTestNGSpringContextTests {

  protected static final Logger LOG = LoggerFactory.getLogger(AbstractRhinoTest.class);

  protected static final Joiner NO_SPACE_JOINER = Joiner.on("").skipNulls();

  public static final String TEST_RHINO_YAML = "rhino-test.yaml";

  @Bean
  public HibernateTemplate hibernateTemplate() {
    LOG.debug("hibernateTemplate() *");
    final HibernateTemplate hibernateTemplate = mock(HibernateTemplate.class);
    return hibernateTemplate;
  }

  @Bean
  public ConfigurationReadService configurationReadService() {
    LOG.debug("configurationReadService() *");
    final ConfigurationReadService configurationReadService = mock(ConfigurationReadService.class);
    return configurationReadService;
  }

  @Bean
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
