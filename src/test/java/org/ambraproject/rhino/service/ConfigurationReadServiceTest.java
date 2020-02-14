/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.service;

import com.google.gson.Gson;
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class ConfigurationReadServiceTest extends BaseRhinoTest {
  @Autowired
  private ConfigurationReadService configurationReadService;

  @Test
  public void testGetBuildProperties() throws IOException {
    Properties actualProperties = configurationReadService.getBuildProperties();

    assertTrue("Did not contain version", actualProperties.containsKey("version"));
    assertTrue("Did not contain buildDate", actualProperties.containsKey("buildDate"));
    assertTrue("Did not contain buildUser", actualProperties.containsKey("buildUser"));
    assertTrue("Did not contain gitCommitIdAbbrev", actualProperties.containsKey("gitCommitIdAbbrev"));
  }

  @Test
  public void testReadBuildProperties() throws IOException {
    final ServiceResponse<Properties> buildProperties = configurationReadService.readBuildConfig();
    assertNotNull(buildProperties);
    final ResponseEntity<?> responseEntity = buildProperties.asJsonResponse(new Gson());
    assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
  }

  @Test
  public void testGetRepoConfig() throws IOException {
    Map<String, Object> repoConfigMap = configurationReadService.getRepoConfig();
    Map<String, Object> editorialConfigMap = (Map<String, Object>) repoConfigMap.get("editorial");

    String repoAddress = editorialConfigMap.get("address").toString();
    assertEquals("Invalid/missing content repo URL", "http://path/to/content/repo", repoAddress);
    String repoBucket = editorialConfigMap.get("bucket").toString();
    assertEquals("Invalid/missing content repo bucket name", "bucket_name", repoBucket);

  }

  @Test
  public void testReadRepoConfig() throws IOException {
    final ServiceResponse<Map<String, Object>> repoConfig = configurationReadService.readRepoConfig();
    assertNotNull(repoConfig);
    final ResponseEntity<?> responseEntity = repoConfig.asJsonResponse(new Gson());
    assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
  }

  @Test
  public void testReadRunInfo() throws IOException {
    final ServiceResponse<Map<String, String>> runConfig = configurationReadService.readRunInfo();
    assertResponseEntity(runConfig);
  }

  private void assertResponseEntity(ServiceResponse<Map<String, String>> config) throws IOException {
    assertNotNull(config);
    final ResponseEntity<?> responseEntity = config.asJsonResponse(new Gson());
    assertEquals(responseEntity.getStatusCode(), HttpStatus.OK);
  }

}
