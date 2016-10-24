package org.ambraproject.rhino.service;


import com.google.common.collect.ImmutableSet;
import org.ambraproject.rhino.util.response.Transceiver;

import java.io.IOException;
import java.util.Properties;

public interface ConfigurationReadService {

  public static final ImmutableSet<String> CONFIG_TYPES = ImmutableSet.of("build", "repo");

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
  public Transceiver readBuildConfig() throws IOException;


  /**
   * Respond with a JSON object containing all content repository-related Rhino config values.
   */
  public Transceiver readRepoConfig() throws IOException;
}
