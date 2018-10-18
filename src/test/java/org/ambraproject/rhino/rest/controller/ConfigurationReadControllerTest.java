package org.ambraproject.rhino.rest.controller;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.Assert.fail;

import java.net.URI;

import org.ambraproject.rhino.AbstractRhinoTest;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.ambraproject.rhino.service.impl.ConfigurationReadServiceImpl;
import org.ambraproject.rhino.util.GitInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@ContextConfiguration(
    classes = {ConfigurationReadController.class, ConfigurationReadControllerTest.class})
@WebAppConfiguration
@Configuration
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class ConfigurationReadControllerTest extends AbstractRhinoTest {

  private static final JsonParser jsonParser = new JsonParser();

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockModelViewController;

  private RuntimeConfiguration mockRuntimeConfiguration;

  private GitInfo mockGitInfo;

  @Bean
  protected GitInfo gitInfo() {
    final GitInfo gitInfo = mock(GitInfo.class);
    return gitInfo;
  }

  @Bean
  public ConfigurationReadService configurationReadService() {
    LOG.info("configurationReadService() *");
    final ConfigurationReadService configurationReadService = spy(
        ConfigurationReadServiceImpl.class);
    return configurationReadService;
  }

  /**
   * Initialize test data fixtures.
   */
  @Before
  public void init() {
    mockGitInfo = applicationContext.getBean(GitInfo.class);
    mockRuntimeConfiguration = applicationContext.getBean(RuntimeConfiguration.class);
    mockModelViewController = MockMvcBuilders.webAppContextSetup(context).build();
  }

  /**
   * Test request for <b>build</b> configuration should succeed.
   *
   * @throws Exception if API request fails
   */
  @Test
  public void testReadBuildConfigShouldSucceed() throws Exception {
    final String expectedGitCommId = "0000_commit_id";
    doReturn(expectedGitCommId).when(mockGitInfo).getCommitIdAbbrev();

    final MvcResult result = mockModelViewController.perform(get(new URI("/config?type=build")))
        .andExpect(status().isOk()).andReturn();

    final MockHttpServletResponse response = result.getResponse();
    final JsonObject config = jsonParser.parse(response.getContentAsString()).getAsJsonObject();
    assertThat(config.getAsJsonPrimitive("gitCommitIdAbbrev").getAsString())
        .isEqualTo(expectedGitCommId);
    assertThat(config.has("version")).isTrue();
    assertThat(config.has("buildUser")).isTrue();
    assertThat(config.has("buildDate")).isTrue();

    verify(mockGitInfo).getCommitIdAbbrev();
  }

  /**
   * Test request for <b>repo</b> configuration should succeed.
   *
   * @throws Exception if API request fails
   */
  @Test
  public void testReadRepoConfigShouldSucceed() throws Exception {
    final MvcResult result = mockModelViewController.perform(get(new URI("/config?type=repo")))
        .andExpect(status().isOk()).andReturn();
    final MockHttpServletResponse response = result.getResponse();
    final JsonObject data = jsonParser.parse(response.getContentAsString()).getAsJsonObject();

    final JsonObject corpus = data.getAsJsonObject("corpus");
    assertThat(corpus).isNotNull();
    assertThat(corpus.getAsJsonPrimitive("address").getAsString())
        .isEqualTo("http://path/to/content/repo");

    final JsonArray actualSecondaryBuckets = corpus.getAsJsonArray("secondaryBuckets");
    assertThat(actualSecondaryBuckets).hasSize(1);
    assertThat(actualSecondaryBuckets.getAsString()).isEqualTo("secondary_bucket");
  }

  /**
   * Test request for <b>run</b> info should succeed.
   *
   * @throws Exception if API request fails
   */
  @Test
  public void testReadRunInfoShouldSucceed() throws Exception {
    final MvcResult result = mockModelViewController.perform(get(new URI("/config?type=run")))
        .andExpect(status().isOk()).andReturn();
    final MockHttpServletResponse response = result.getResponse();
    final JsonObject data = jsonParser.parse(response.getContentAsString()).getAsJsonObject();
    assertThat(data.has("host")).isTrue();
    assertThat(data.has("started")).isTrue();
  }

  /**
   * Test request for invalid configuration <b>type</b> should fail.
   *
   * @throws Exception if API request fails
   */
  @Test
  public void testInvalidConfigTypeShouldFail() throws Exception {
    try {
      mockModelViewController.perform(get(new URI("/config?type=unknown")));
      fail("Expecting exception, but nothing was thrown.");
    } catch (Throwable exception) {
      assertThat(exception.getCause()).isInstanceOf(RestClientException.class);
    }
  }

  /**
   * Test failed request to read API Configurations.
   *
   * @throws Exception if API request fails 
   */
  @Test
  public void testReadUserApiConfigNotFound() throws Exception {
    mockModelViewController.perform(get(new URI("/config/userApi/")))
        .andExpect(status().isNotFound());
    mockModelViewController.perform(get(new URI("/config/userApi")))
        .andExpect(status().isNotFound());

   verify(mockRuntimeConfiguration, times(0)).getNedConfiguration();
  }
}
