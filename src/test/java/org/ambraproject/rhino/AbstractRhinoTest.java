package org.ambraproject.rhino;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.InputStream;

import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.config.YamlConfiguration;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.ambraproject.rhino.service.HibernatePersistenceService;
import org.ambraproject.rhino.util.Java8TimeGsonAdapters;
import org.ambraproject.rhino.util.JsonAdapterUtil;
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
}