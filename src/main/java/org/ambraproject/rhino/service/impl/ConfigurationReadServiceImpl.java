package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.io.Closer;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class ConfigurationReadServiceImpl extends AmbraService implements ConfigurationReadService {

  @Autowired
  private Configuration ambraConfiguration;

  @Override
  public void read(ResponseReceiver receiver, MetadataFormat format) throws IOException {
    serializeMetadata(format, receiver, convertToMap(ambraConfiguration));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Properties getBuildProperties() throws IOException {
    Properties properties = new Properties();
    Closer closer = Closer.create();
    try {
      InputStream is = closer.register(getClass().getResourceAsStream("/version.properties"));
      properties.load(is);
    } catch (Throwable t) {
      throw closer.rethrow(t);
    } finally {
      closer.close();
    }
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
