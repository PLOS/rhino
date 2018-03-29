package org.ambraproject.rhino.rest.controller;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.Map;
import java.util.Properties;

import org.ambraproject.rhino.AbstractRhinoTest;
import org.ambraproject.rhino.config.RuntimeConfiguration;
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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;

@ContextConfiguration(
    classes = {ConfigurationReadController.class, ConfigurationReadControllerTest.class})
@Configuration
@WebAppConfiguration
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class ConfigurationReadControllerTest extends AbstractRhinoTest {

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockModelViewController;

  private RuntimeConfiguration mockRuntimeConfiguration;

  private ConfigurationReadService mockConfigurationReadService;

  private Gson entityGson;

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
  @BeforeMethod(alwaysRun = true)
  public void init() {
    entityGson = applicationContext.getBean(Gson.class);
    mockRuntimeConfiguration = applicationContext.getBean(RuntimeConfiguration.class);
    mockConfigurationReadService = applicationContext.getBean(ConfigurationReadService.class);
    mockModelViewController = MockMvcBuilders.webAppContextSetup(context).build();
  }

  /**
   * Test request for <b>build</b> configuration should succeed.
   *
   * @throws Exception if API request fails
   */
  @Test
  public void testReadBuildConfigShouldSucceed() throws Exception {
    final Properties buildProperties = new Properties();
    buildProperties.setProperty("version", "0.0.0");
    buildProperties.setProperty("buildUser", "teamcity");
    buildProperties.setProperty("buildDate", "YYYMMDD");
    buildProperties.setProperty("gitCommitIdAbbrev", "commit_id");
    doReturn(buildProperties).when(mockConfigurationReadService).getBuildProperties();

    final MvcResult result = mockModelViewController.perform(get(new URI("/config?type=build")))
        .andExpect(status().isOk()).andReturn();
    final MockHttpServletResponse response = result.getResponse();
    final String expectedProperties = entityGson.toJson(buildProperties);
    assertThat(response.getContentAsString()).isEqualTo(expectedProperties);
  }

  /**
   * Test request for <b>repo</b> configuration should succeed.
   *
   * @throws Exception if API request fails
   */
  @Test
  public void testReadRepoConfigShouldSucceed() throws Exception {
    final ImmutableMap<String, Map<String, Object>> repoConfig = ImmutableMap.of(
        "editorial", ImmutableMap.of("bucket", "plive"),
        "corpus", ImmutableMap.of("secondaryBuckets", ImmutableList.of()));
    doReturn(repoConfig).when(mockConfigurationReadService).getRepoConfig();

    final MvcResult result = mockModelViewController.perform(get(new URI("/config?type=repo")))
        .andExpect(status().isOk()).andReturn();
    final MockHttpServletResponse response = result.getResponse();
    final String expectedConfig = entityGson.toJson(repoConfig);
    assertThat(response.getContentAsString()).isEqualTo(expectedConfig);
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
