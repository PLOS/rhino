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


import com.google.common.base.Preconditions;
import org.ambraproject.rhino.identity.ArticleIdentity;
import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.service.IngestibleService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileNotFoundException;
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
public class IngestibleServiceImpl extends AmbraService implements IngestibleService {

  private static final String INGEST_SOURCE_DIR_KEY
      = "ambra.services.documentManagement.ingestSourceDir";

  private static final String INGEST_DEST_DIR_KEY
      = "ambra.services.documentManagement.ingestDestinationDir";

  private static final Pattern ARCHIVE_FILE_RE = Pattern.compile("p[a-z]{3}\\.\\d{7}\\.zip");

  @Autowired
  private Configuration ambraConfiguration;

  /**
   * {@inheritDoc}
   */
  @Override
  public void read(ResponseReceiver receiver, MetadataFormat format) throws IOException {
    Preconditions.checkArgument(format == MetadataFormat.JSON);
    String ingestSourceDirName = ambraConfiguration.getString(INGEST_SOURCE_DIR_KEY);
    Preconditions.checkNotNull(ingestSourceDirName); // should be covered by webapp's built-in defaults
    File ingestDir = new File(ingestSourceDirName);
    File[] archives = ingestDir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        Matcher m = ARCHIVE_FILE_RE.matcher(name.toLowerCase());
        return m.matches();
      }
    });
    if (archives == null) {
      throw new RuntimeException("Directory not found: " + ingestDir);
    }

    List<String> results = new ArrayList<String>(archives.length);
    for (File archive : archives) {

      // Since the rest of the API uses DOIs as identifiers, we do here as well, instead
      // of the .zip archive filename.
      String filename = archive.getName();
      results.add("info:doi/10.1371/journal." + filename.substring(0, filename.length() - 4));
    }
    Collections.sort(results);
    writeJson(receiver, results);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public File getIngestibleArchive(ArticleIdentity articleIdentity) throws IOException {
    return getIngestSourceArchive(articleIdentity);
  }

  private File getIngestSourceArchive(ArticleIdentity articleIdentity) throws IOException {
    String doi = articleIdentity.getIdentifier();
    File result = new File(ambraConfiguration.getString(INGEST_SOURCE_DIR_KEY) + File.separator
        + doi.substring(doi.length() - "pfoo.1234567".length()) + ".zip");
    if (!result.canRead()) {
      throw new FileNotFoundException("Archive not found: " + result.getCanonicalPath());
    }
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public File archiveIngested(ArticleIdentity articleIdentity) throws IOException {
    File source = getIngestSourceArchive(articleIdentity);
    String doi = articleIdentity.getIdentifier();
    File dest = new File(ambraConfiguration.getString(INGEST_DEST_DIR_KEY) + File.separator
        + doi.substring(doi.length() - "pfoo.1234567".length()) + ".zip");
    if (!source.renameTo(dest)) {
      throw new IOException(String.format("Could not move %s to %s", source.getCanonicalPath(),
          dest.getCanonicalPath()));
    }
    return dest;
  }
}
