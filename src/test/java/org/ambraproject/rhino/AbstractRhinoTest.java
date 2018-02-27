package org.ambraproject.rhino;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.InputStream;

import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.config.YamlConfiguration;
import org.ambraproject.rhino.content.xml.CustomMetadataExtractor;
import org.ambraproject.rhino.service.HibernatePersistenceService;
import org.plos.crepo.service.ContentRepoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.yaml.snakeyaml.Yaml;

import com.google.gson.Gson;

/**
 * Abstract base class for Rhino unit tests.
 */
public class AbstractRhinoTest extends AbstractTestNGSpringContextTests {

  protected static final Logger LOG = LoggerFactory.getLogger(AbstractRhinoTest.class);

  public static final String TEST_RHINO_YAML = "rhino-test.yaml";

  protected HibernateTemplate hibernateTemplate;

  protected HibernatePersistenceService hibernatePersistenceService;

  protected ContentRepoService contentRepoService;

  protected Gson entityGson;

  @BeforeClass(alwaysRun = true)
  public void initMocks() {
    hibernateTemplate = mock(HibernateTemplate.class);
    hibernatePersistenceService = mock(HibernatePersistenceService.class);
    contentRepoService = mock(ContentRepoService.class);
    entityGson = mock(Gson.class);
  }

  @Bean
  public HibernateTemplate hibernateTemplate() {
    LOG.debug("hibernateTemplate() *");
    return hibernateTemplate;
  }

  @Bean
  public HibernatePersistenceService hibernatePersistenceService() {
    LOG.debug("HibernatePersistenceService() *");
    return hibernatePersistenceService;
  }

  @Bean
  public Gson entityGson() {
    LOG.debug("entityGson() *");
    return entityGson;
  }

  @Bean
  public ContentRepoService contentRepoService() {
    LOG.debug("contentRepoService() *");
    return contentRepoService;
  }

  @Bean
  public RuntimeConfiguration runtimeConfiguration(Yaml yaml) throws Exception {
    try (final InputStream is =
        AbstractRhinoTest.class.getClassLoader().getResourceAsStream(TEST_RHINO_YAML)) {
      final YamlConfiguration runtimeConfiguration =
          new YamlConfiguration(yaml.loadAs(is, YamlConfiguration.Input.class));
      LOG.debug("runtimeConfiguration: Loaded {}", TEST_RHINO_YAML);
      return runtimeConfiguration;
    } catch (Exception exception) {
      LOG.warn("runtimeConfiguration: Caught exception: {}", exception);
    }
    return null;
  }

  @Bean
  public Yaml yaml() {
    return new Yaml();
  }
}
