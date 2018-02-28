package org.ambraproject.rhino;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.InputStream;

import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.config.YamlConfiguration;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Abstract base class for Rhino unit tests.
 */
public class AbstractRhinoTest extends AbstractTestNGSpringContextTests {

  protected static final Logger LOG = LoggerFactory.getLogger(AbstractRhinoTest.class);

  public static final String TEST_RHINO_YAML = "rhino-test.yaml";

  @Bean
  public HibernateTemplate hibernateTemplate() {
    LOG.debug("hibernateTemplate() *");
    final HibernateTemplate hibernateTemplate = mock(HibernateTemplate.class);
    return hibernateTemplate;
  }

  @Bean
  public HibernatePersistenceService hibernatePersistenceService() {
    LOG.debug("HibernatePersistenceService() *");
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
      LOG.debug("runtimeConfiguration: Loaded {}", TEST_RHINO_YAML);
      return spy(runtimeConfiguration);
    } catch (Exception exception) {
      LOG.warn("runtimeConfiguration: Caught exception: {}", exception);
    }
    return mock(YamlConfiguration.class);
  }

  @Bean
  public Yaml yaml() {
    return new Yaml();
  }
}
