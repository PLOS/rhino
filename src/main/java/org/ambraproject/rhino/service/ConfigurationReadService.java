package org.ambraproject.rhino.service;


import org.ambraproject.rhino.util.response.MetadataRetriever;

import java.io.IOException;
import java.util.Properties;

public interface ConfigurationReadService {

  /**
   * Write all values from the Ambra configuration.
   *
   * @throws IOException
   */
  public abstract MetadataRetriever read() throws IOException;

  /**
   * Returns a {@link Properties} instance with information about how the application was built.
   *
   * @return Properties object that will contain at least three entries: "version", specifying the version in the
   * pom.xml; "buildDate", specifying when the build occurred; and "buildUser", specifying who performed the build
   * @throws IOException
   */
  public abstract Properties getBuildProperties() throws IOException;

  /**
   * Respond with a JSON object containing all values from {@link #getBuildProperties()}.
   */
  public MetadataRetriever readBuildProperties() throws IOException;

}
