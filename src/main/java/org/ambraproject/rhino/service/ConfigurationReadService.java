package org.ambraproject.rhino.service;

import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.util.response.ResponseReceiver;

import java.io.IOException;

public interface ConfigurationReadService {

  /**
   * Write all values from the Ambra configuration.
   *
   * @param receiver the response to which to write the configuration values
   * @param format   the format in which to write
   * @throws IOException
   */
  public abstract void read(ResponseReceiver receiver, MetadataFormat format) throws IOException;

}
