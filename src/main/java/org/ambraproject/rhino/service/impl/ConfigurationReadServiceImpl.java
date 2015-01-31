package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.ambraproject.rhino.util.GitInfo;
import org.ambraproject.rhino.util.response.Transceiver;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigurationReadServiceImpl extends AmbraService implements ConfigurationReadService {

  @Autowired
  private Configuration ambraConfiguration;

  @Autowired
  private RuntimeConfiguration runtimeConfiguration;

  @Autowired
  private GitInfo gitInfo;

  @Override
  public Transceiver readAmbraConfig() throws IOException {
    return new Transceiver() {
      @Override
      protected Object getData() throws IOException {
        return convertToMap(ambraConfiguration);
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  @Override
  public Transceiver readBuildConfig() throws IOException {
    return new Transceiver() {
      @Override
      protected Object getData() throws IOException {
        return getBuildProperties();
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
  }

  private static Map<String, Object> showEndpointAsMap(RuntimeConfiguration.ContentRepoEndpoint endpoint) {
    if (endpoint == null) return null;
    Map<String, Object> map = new LinkedHashMap<>(4);
    map.put("address", endpoint.getAddress());
    map.put("bucket", endpoint.getBucket());
    return map;
  }

  @Override
  public Transceiver readRepoConfig() throws IOException {
    return new Transceiver() {
      @Override
      protected Object getData() throws IOException {
        Map<String, Object> cfgMap = new LinkedHashMap<>(4);
        cfgMap.put("editorial", showEndpointAsMap(runtimeConfiguration.getEditorialBucket()));
        cfgMap.put("corpus", showEndpointAsMap(runtimeConfiguration.getCorpusBucket()));
        return cfgMap;
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    };
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

  /**
   * Represent a {@link Configuration} object as a map from its keys onto its properties.
   *
   * @param configuration
   * @return
   */
  private Map<String, Object> convertToMap(Configuration configuration) throws IOException {
    Preconditions.checkNotNull(configuration);
    Map<String, Object> map = Maps.newLinkedHashMap();
    Properties buildProperties = getBuildProperties();
    for (String key : buildProperties.stringPropertyNames()) {
      map.put(key, buildProperties.get(key));
    }
    for (Iterator<?> iterator = configuration.getKeys(); iterator.hasNext(); ) {
      String key = (String) iterator.next();
      Object property = configuration.getProperty(key);
      map.put(key, property);
    }
    return map;
  }
}
