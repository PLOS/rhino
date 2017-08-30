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

import org.ambraproject.rhino.BaseRhinoTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


public class ConfigurationReadServiceTest extends BaseRhinoTest {
  @Autowired
  private ConfigurationReadService configurationReadService;

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
    Map<String, Object> repoConfigMap = configurationReadService.getRepoConfig();
    Map<String, Object> editorialConfigMap = (Map<String, Object>) repoConfigMap.get("editorial");

    String repoAddress = editorialConfigMap.get("address").toString();
    assertEquals(repoAddress, "http://path/to/content/repo", "Invalid/missing content repo URL");
    String repoBucket = editorialConfigMap.get("bucket").toString();
    assertEquals(repoBucket, "bucket_name", "Invalid/missing content repo bucket name");

    Map<String, Object> corpusConfigMap = (Map<String, Object>) repoConfigMap.get("corpus");
    final Set<String> secondaryBuckets = (Set<String>) corpusConfigMap.get("secondaryBuckets");
    assertEquals(secondaryBuckets.iterator().next(), "secondary_bucket");
  }

}
