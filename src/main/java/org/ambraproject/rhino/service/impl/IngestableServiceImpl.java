/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2013 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.service.impl;

import com.google.inject.internal.Preconditions;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.service.IngestableService;
import org.apache.commons.configuration.Configuration;
import org.topazproject.ambra.configuration.ConfigurationStore;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@inheritDoc}
 */
public class IngestableServiceImpl extends AmbraService implements IngestableService {

  private static final Pattern ARCHIVE_FILE_RE = Pattern.compile("p[a-z]{3}\\.\\d{7}\\.zip");

  private Configuration config = ConfigurationStore.getInstance().getConfiguration();

  /**
   * {@inheritDoc}
   */
  @Override
  public void read(HttpServletResponse response, MetadataFormat format) throws IOException {
    Preconditions.checkArgument(format == MetadataFormat.JSON);
    File ingestDir = new File(config.getString(
        "ambra.services.documentManagement.ingestSourceDir"));
    File[] archives = ingestDir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        Matcher m = ARCHIVE_FILE_RE.matcher(name.toLowerCase());
        return m.matches();
      }
    });

    List<String> results = new ArrayList<String>(archives.length);
    for (File archive : archives) {

      // Since the rest of the API uses DOIs as identifiers, we do here as well, instead
      // of the .zip archive filename.
      String filename = archive.getName();
      results.add("info:doi/10.1371/journal." + filename.substring(0, filename.length() - 4));
    }
    Collections.sort(results);
    writeJsonToResponse(response, results);
  }
}
