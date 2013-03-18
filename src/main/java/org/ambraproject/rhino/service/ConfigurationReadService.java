package org.ambraproject.rhino.service;

import org.ambraproject.rhino.rest.MetadataFormat;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface ConfigurationReadService {

  /**
   * Write all values from the Ambra configuration.
   *
   * @param response the response to which to write the configuration values
   * @param format   the format in which to write
   * @throws IOException
   */
  public abstract void read(HttpServletResponse response, MetadataFormat format) throws IOException;

}
