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

/**
 * {@inheritDoc}
 */
public class IngestibleServiceImpl extends AmbraService implements IngestibleService {

  private static final String INGEST_SOURCE_DIR_KEY
      = "ambra.services.documentManagement.ingestSourceDir";

  private static final String INGEST_DEST_DIR_KEY
      = "ambra.services.documentManagement.ingestDestinationDir";

  private static final FilenameFilter ZIP_FILENAME_FILTER = new FilenameFilter() {
    @Override
    public boolean accept(File dir, String name) {
      return name.endsWith(".zip");
    }
  };

  @Autowired
  private Configuration ambraConfiguration;

  /**
   * {@inheritDoc}
   */
  @Override
  public void read(ResponseReceiver receiver, MetadataFormat format) throws IOException {
    String ingestSourceDirName = ambraConfiguration.getString(INGEST_SOURCE_DIR_KEY);
    Preconditions.checkNotNull(ingestSourceDirName); // should be covered by webapp's built-in defaults
    File ingestDir = new File(ingestSourceDirName);
    File[] archives = ingestDir.listFiles(ZIP_FILENAME_FILTER);
    if (archives == null) {
      throw new RuntimeException("Directory not found: " + ingestDir);
    }

    List<String> results = new ArrayList<>(archives.length);
    for (File archive : archives) {
      results.add(archive.getName());
    }
    Collections.sort(results);
    serializeMetadata(format, receiver, results);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public File getIngestibleArchive(String filename) throws IOException {
    return getIngestSourceArchive(filename);
  }

  private File getIngestSourceArchive(String filename) throws IOException {
    File result = new File(ambraConfiguration.getString(INGEST_SOURCE_DIR_KEY), filename);
    if (!result.canRead()) {
      throw new FileNotFoundException("Archive not found: " + result.getCanonicalPath());
    }
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public File archiveIngested(String filename) throws IOException {
    File source = getIngestSourceArchive(filename);
    File dest = new File(ambraConfiguration.getString(INGEST_DEST_DIR_KEY), filename);
    if (!source.renameTo(dest)) {
      throw new IOException(String.format("Could not move %s to %s", source.getCanonicalPath(),
          dest.getCanonicalPath()));
    }
    return dest;
  }
}
