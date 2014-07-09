/*
 * Copyright (c) 2006-2014 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.service;

import com.google.gson.Gson;
import org.ambraproject.rhino.BaseRhinoTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.util.Properties;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class ConfigurationReadServiceTest extends BaseRhinoTest {
  @Autowired
  private ConfigurationReadService configurationReadService;

  @Autowired
  protected Gson entityGson;

  @Test
  public void testGetBuildProperties() throws IOException {
    Properties actualProperties = configurationReadService.getBuildProperties();

    assertTrue(actualProperties.containsKey("version"), "Did not contain version");
    assertTrue(actualProperties.containsKey("buildDate"), "Did not contain buildDate");
    assertTrue(actualProperties.containsKey("buildUser"), "Did not contain buildUser");
    assertTrue(actualProperties.containsKey("gitCommitIdAbbrev"), "Did not contain gitCommitIdAbbrev");
  }

  @Test
  public void testReadRepoConfig() throws IOException {
    String repoConfigJson = configurationReadService.readRepoConfig().readJson(entityGson);
    assertTrue(repoConfigJson.length() > 0, "ConfigurationReadService did not return content repo configuration");
    Map<String,Object> repoConfigMap = entityGson.fromJson(repoConfigJson, HashMap.class);
    String repoAddress = repoConfigMap.get("contentRepoAddress").toString();
    assertEquals(repoAddress, "http://path/to/content/repo", "Invalid/missing content repo URL");
    assertEquals(repoConfigMap.get("repoBucketName").toString(),"bucket_name","Invalid/missing content repo bucket name");
  }

}
