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

package org.ambraproject.rhino.service.impl;

import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.ambraproject.rhino.util.GitInfo;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigurationReadServiceImpl extends AmbraService implements ConfigurationReadService {

  @Autowired
  private RuntimeConfiguration runtimeConfiguration;

  @Autowired
  private GitInfo gitInfo;

  @Override
  public ServiceResponse<Properties> readBuildConfig() throws IOException {
    return ServiceResponse.serveView(getBuildProperties());
  }

  private static Map<String, Object> showEndpointAsMap(RuntimeConfiguration.ContentRepoEndpoint endpoint) {
    if (endpoint == null) return null;
    Map<String, Object> map = new LinkedHashMap<>(4);
    map.put("address", endpoint.getAddress());
    map.put("bucket", endpoint.getDefaultBucket());
    if (endpoint instanceof RuntimeConfiguration.MultiBucketContentRepoEndpoint) {
      map.put("secondaryBuckets", ((RuntimeConfiguration.MultiBucketContentRepoEndpoint) endpoint).getAllBuckets());
    }
    return map;
  }

  @Override
  public ServiceResponse<Map<String, Object>> readRepoConfig() throws IOException {
    return ServiceResponse.serveView(getRepoConfig());
  }

  @Override
  public Map<String, Object> getRepoConfig() {
    Map<String, Object> cfgMap = new LinkedHashMap<>(4);
    cfgMap.put("editorial", showEndpointAsMap(runtimeConfiguration.getEditorialStorage()));
    cfgMap.put("corpus", showEndpointAsMap(runtimeConfiguration.getCorpusStorage()));
    return cfgMap;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Properties getBuildProperties() throws IOException {
    Properties properties = new Properties();
    try (InputStream is = getClass().getResourceAsStream("/version.properties")) {
      properties.load(is);
    }

    properties.setProperty("gitCommitIdAbbrev", gitInfo.getCommitIdAbbrev());
    return properties;
  }

}
