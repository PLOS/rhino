package org.ambraproject.rhino.service;

import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.util.response.ResponseReceiver;

import java.io.IOException;
import java.util.Properties;

public interface ConfigurationReadService {

  /**
   * Write all values from the Ambra configuration.
   *
   * @param receiver the response to which to write the configuration values
   * @param format   the format in which to write
   * @throws IOException
   */
  public abstract void read(ResponseReceiver receiver, MetadataFormat format) throws IOException;

  /**
   * Returns a {@link Properties} instance with information about how the application
   * was built.
   *
   * @return Properties object that will contain at least three entries: "version", specifying
   *     the version in the pom.xml; "buildDate", specifying when the build occurred; and
   *     "buildUser", specifying who performed the build
   * @throws IOException
   */
  public abstract Properties getBuildProperties() throws IOException;
}
