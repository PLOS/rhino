package org.ambraproject.rhino.service.impl;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.ambraproject.rhino.AbstractRhinoTest;
import org.ambraproject.rhino.service.ArticleCrudService;
import org.ambraproject.rhino.service.ArticleRevisionWriteService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link ArticleRevisionWriteServiceImpl}.
 */
@ContextConfiguration(classes = ArticleRevisionWriteServiceTest.class)
@Configuration
public class ArticleRevisionWriteServiceTest extends AbstractRhinoTest {

  /**
   * Prepare test fixtures.
   */
  @BeforeMethod(alwaysRun = true)
  public void init() {
  }

  @Bean
  public ArticleCrudService articleCrudService() {
    final ArticleCrudService articleCrudService = mock(ArticleCrudService.class);
    return articleCrudService;
  }

  @Bean
  public ArticleRevisionWriteService articleRevisionWriteService() {
    final ArticleRevisionWriteService articleRevisionWriteService = spy(new ArticleRevisionWriteServiceImpl());
    return articleRevisionWriteService;
  }

  /**
   * Test successful creation of an article revision.
   */
  @Test
  public void testCreateRevisionShouldSucceed() {
    final ArticleRevisionWriteService mockArticleRevisionWriteService =
        applicationContext.getBean(ArticleRevisionWriteService.class);

  }
}
