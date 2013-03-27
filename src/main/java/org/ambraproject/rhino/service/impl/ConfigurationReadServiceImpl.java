package org.ambraproject.rhino.service.impl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class ConfigurationReadServiceImpl extends AmbraService implements ConfigurationReadService {

  @Autowired
  private Configuration ambraConfiguration;

  @Override
  public void read(ResponseReceiver receiver, MetadataFormat format) throws IOException {
    Preconditions.checkNotNull(format);
    assert format == MetadataFormat.JSON;
    writeJson(receiver, convertToMap(ambraConfiguration));
  }

  /**
   * Represent a {@link Configuration} object as a map from its keys onto its properties.
   *
   * @param configuration
   * @return
   */
  private static Map<String, Object> convertToMap(Configuration configuration) {
    Preconditions.checkNotNull(configuration);
    Map<String, Object> map = Maps.newLinkedHashMap();
    for (Iterator<?> iterator = configuration.getKeys(); iterator.hasNext(); ) {
      String key = (String) iterator.next();
      Object property = configuration.getProperty(key);
      map.put(key, property);
    }
    return map;
  }

}
