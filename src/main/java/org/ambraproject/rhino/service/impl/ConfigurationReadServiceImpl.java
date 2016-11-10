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
    Map<String, Object> cfgMap = new LinkedHashMap<>(4);
    cfgMap.put("editorial", showEndpointAsMap(runtimeConfiguration.getEditorialStorage()));
    cfgMap.put("corpus", showEndpointAsMap(runtimeConfiguration.getCorpusStorage()));
    return ServiceResponse.serveView(cfgMap);
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
