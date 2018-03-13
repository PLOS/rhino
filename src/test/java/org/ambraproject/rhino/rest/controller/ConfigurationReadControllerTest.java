package org.ambraproject.rhino.rest.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.ambraproject.rhino.AbstractRhinoTest;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@ContextConfiguration(classes = ConfigurationReadControllerTest.class)
@Configuration
@WebAppConfiguration
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class ConfigurationReadControllerTest extends AbstractRhinoTest {

  @Autowired
  private WebApplicationContext webApplicationContext;

  private MockMvc mockModelViewController;

  private RuntimeConfiguration mockRuntimeConfiguration;

  /**
   * Initialize test data fixtures.
   *
   * @throws MalformedURLException if improper URL 
   */
  @BeforeMethod(alwaysRun = true)
  public void init() {
    mockRuntimeConfiguration = applicationContext.getBean(RuntimeConfiguration.class);

    mockModelViewController = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
  }

  /**
   * Test failed request to read API Configurations.
   *
   * @throws Exception if API request fails 
   */
  @Test
  public void testReadUserApiConfigShouldFail() throws Exception {
    mockModelViewController.perform(get("/config/userApi/")).andExpect(status().isNotFound());
    mockModelViewController.perform(get("/config/userApi")).andExpect(status().isNotFound());

   verify(mockRuntimeConfiguration, times(0)).getNedConfiguration();
  }
}
