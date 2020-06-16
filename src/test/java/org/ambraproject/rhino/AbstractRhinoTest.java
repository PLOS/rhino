package org.ambraproject.rhino;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import java.util.UUID;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.ambraproject.rhino.service.ArticleDatabaseService;
import org.ambraproject.rhino.util.Java8TimeGsonAdapters;
import org.ambraproject.rhino.util.JsonAdapterUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.function.SQLFunctionRegistry;
import org.hibernate.engine.SessionFactoryImplementor;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * Abstract base class for Rhino unit tests.
 */
public abstract class AbstractRhinoTest extends AbstractJUnit4SpringContextTests {

  protected static final Logger LOG = LogManager.getLogger(AbstractRhinoTest.class);

  protected static final String DESTINATION_BUCKET = "water_bucket";

  protected static final Joiner NO_SPACE_JOINER = Joiner.on("").skipNulls();

  /**
   * Flag to determine if <b>spying</b> on the
   * {@link org.springframework.orm.hibernate3.HibernateTemplate HibernateTemplate}.
   */
  private boolean spyOnHibernateTemplate;

  /**
   * Creates an instance of <code>ArticleDatabaseServiceTest</code>.
   */
  protected AbstractRhinoTest() {
  }

  /**
   * Creates an instance of <code>ArticleDatabaseServiceTest</code>.
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
  public ArticleDatabaseService articleDatabaseService() {
    LOG.debug("articleDatabaseService() *");
    final ArticleDatabaseService articleDatabaseService =
        mock(ArticleDatabaseService.class);
    return articleDatabaseService;
  }

  @Bean
  public Gson entityGson() {
    LOG.debug("entityGson() *");
    final GsonBuilder builder = JsonAdapterUtil.makeGsonBuilder().setPrettyPrinting();
    Java8TimeGsonAdapters.register(builder);
    return builder.create();
  }


  @Bean
  public RuntimeConfiguration runtimeConfiguration() throws Exception {
    return mock(RuntimeConfiguration.class);
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
   * Method to mock a
   * {@link org.springframework.orm.hibernate3.HibernateTemplate HibernateTemplate}.
   *
   * @param answer The {@link org.hibernate.Query Query} to associate with
   *              {@link org.hibernate.classic.Session#createQuery(String) createQuery()}
   *
   * @return The {@link org.springframework.orm.hibernate3.HibernateTemplate HibernateTemplate}
   */
  public HibernateTemplate buildMockHibernateTemplate(Answer<Query> answer) {
    Preconditions.checkNotNull(answer, "answer reference cannot be null");

    final SessionFactory sessionFactory = applicationContext.getBean(SessionFactory.class);
    if (spyOnHibernateTemplate) {
      final Session mockSession = mock(Session.class);
      when(mockSession.getFlushMode()).thenReturn(FlushMode.AUTO);
      when(mockSession.createQuery(anyString())).then(answer);

      when(sessionFactory.openSession()).thenReturn(mockSession);
    }

    return applicationContext.getBean(HibernateTemplate.class);
  }
}
