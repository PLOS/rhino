/* $HeadURL::                                                                            $
 * $Id$
 *
 * Copyright (c) 2006-2010 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ambraproject.rhino.service;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.OverrideCombiner;
import org.apache.commons.configuration.tree.UnionCombiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;

/**
 * A singleton that manages the load/unload/re-load of Configuration.<p>
 * <p>
 * Configuration consists of a layered set of configuration files where configuration in a higher layer overrides those
 * of the lower layers. Starting from the lowest layer, configuration consists of: <ul>
 * <li><var>/global-defaults.xml</var> - A resource in this library <li><var>/defaults.xml</var> - A resource or
 * resources in libraries and webapps using this lib <li><var>ambra.configuration.overrides</var> - If set, this defines
 * a named resource or URL of a resource that is added to the configuration tree - usually supplementing and overriding
 * settings in <var>/global-defaults.xml</var> and <var>/defaults.xml</var>. <li><var>file:/etc/ambra/ambra.xml</var>
 * (or <var>ambra.configuration</var>) - A set of user overrides in <var>/etc</var>. The name of this file can be
 * changed for webapps that use WebAppInitializer by changing web.xml or by setting the ambra.configuraiton system
 * property. <li>System properties </ul>
 *
 * @author Pradeep Krishnan
 * @author Eric Brown
 */
public class LegacyConfiguration {

  private static final Logger log = LoggerFactory.getLogger(LegacyConfiguration.class);

  /**
   * A property used to define the location of the master set of configuration overrides. This is usually a xml or
   * properties file in /etc somewhere. Note that this must be a URL. (For example: file:///etc/ambra/ambra.xml.)
   */
  private static final String CONFIG_URL_PROPERTY_NAME = "ambra.configuration";

  /**
   * A property used to define overrides. This is primarily to support something like a development mode. If a valid
   * URL, the resource is found from the URL. If not a URL, it is treated as the name of a resource.
   */
  private static final String OVERRIDES_URL = "ambra.configuration.overrides";

  private static final String DEFAULTS_RESOURCE = "ambra/configuration/defaults.xml";

  private final File configDir;

  public LegacyConfiguration(File configDir) throws FileNotFoundException {
    if (!configDir.exists()) throw new FileNotFoundException();
    this.configDir = configDir;
  }

  /**
   * Load/Reload the configuration from the factory config url.
   *
   * @param configURL URL to the config file for ConfigurationFactory
   * @throws ConfigurationException when the config factory configuration has an error
   */
  public static CombinedConfiguration loadConfiguration(URL configURL) throws ConfigurationException {
    CombinedConfiguration root = new CombinedConfiguration(new OverrideCombiner());

    // System properties override everything
    root.addConfiguration(new SystemConfiguration());

    // Load from ambra.configuration -- /etc/... (optional)
    if (configURL != null) {
      try {
        root.addConfiguration(getConfigurationFromUrl(configURL));
        log.info("Added URL '" + configURL + "'");
      } catch (ConfigurationException ce) {
        if (!(ce.getCause() instanceof FileNotFoundException)) {
          throw ce;
        }
        log.info("Unable to open '" + configURL + "'");
      }
    }

    // Add ambra.configuration.overrides (if defined)
    String overrides = System.getProperty(OVERRIDES_URL);
    if (overrides != null) {
      try {
        root.addConfiguration(getConfigurationFromUrl(new URL(overrides)));
        log.info("Added override URL '" + overrides + "'");
      } catch (MalformedURLException mue) {
        // Must not be a URL, so it must be a resource
        addResources(root, overrides);
      }
    }

    CombinedConfiguration defaults = new CombinedConfiguration(new UnionCombiner());
    // Add defaults.xml from classpath
    addResources(defaults, DEFAULTS_RESOURCE);
    // Add journal.xml from journals/journal-name/configuration/journal.xml
    root.addConfiguration(defaults);

    if (log.isDebugEnabled()) {
      log.debug("Configuration dump: " + System.getProperty("line.separator") +
          ConfigurationUtils.toString(root));
    }

    return root;
  }

  /**
   * Use the default commons configuration specified by this library.
   *
   * @throws ConfigurationException when the configuration can't be found.
   */
  public CombinedConfiguration loadDefaultConfiguration() throws ConfigurationException {
    final URL configUrl;

    // Allow JVM level property to override everything else
    String configUrlProperty = System.getProperty(CONFIG_URL_PROPERTY_NAME);
    if (configUrlProperty != null) {
      try {
        configUrl = new URL(configUrlProperty);
      } catch (MalformedURLException e) {
        throw new ConfigurationException("Invalid value of '" + configUrlProperty + "' for '" + CONFIG_URL_PROPERTY_NAME +
            "'. Must be a valid URL.");
      }
    } else {
      File configFile = new File(configDir, "ambra.xml");
      try {
        configUrl = configFile.toURI().toURL();
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
    }

    return loadConfiguration(configUrl);
  }

  /**
   * Given a URL, determine whether it represents properties or xml and load it as a commons-config Configuration
   * instance.
   */
  private static AbstractConfiguration getConfigurationFromUrl(URL url)
      throws ConfigurationException {
    String urlFile = url.getFile();
    if (urlFile.endsWith("properties")) {
      return new PropertiesConfiguration(url);
    } else {
      return new XMLConfiguration(url);
    }
  }

  /**
   * Iterate over all the resources of the given name and add them to our root configuration.
   *
   * @param root     the root configuration to add to
   * @param resource the resource to add
   * @throws ConfigurationException on an error in adding the new config
   */
  private static void addResources(CombinedConfiguration root, String resource)
      throws ConfigurationException {
    Class<?> klass = LegacyConfiguration.class;
    if (resource.startsWith("/")) {
      root.addConfiguration(getConfigurationFromUrl(klass.getResource(resource)));
      log.info("Added resource '" + resource + "' to configuration");
    } else {
      try {
        Enumeration<URL> rs = klass.getClassLoader().getResources(resource);
        while (rs.hasMoreElements()) {
          URL resourceUrl = rs.nextElement();
          root.addConfiguration(getConfigurationFromUrl(resourceUrl));
          log.info("Added resource '" + resourceUrl + "' from path '" + resource + "' to configuration");
        }
      } catch (IOException ioe) {
        throw new Error("Unexpected error loading resources", ioe);
      }
    }
  }

}
