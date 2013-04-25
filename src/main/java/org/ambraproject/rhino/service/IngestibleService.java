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

package org.ambraproject.rhino.service;

import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.util.response.ResponseReceiver;

import java.io.File;
import java.io.IOException;

/**
 * Service that deals with article .zip archives in the ingest source directory (defined by the
 * ambra.services.documentManagement.ingestSourceDir property of ambra.xml).
 */
public interface IngestibleService {

  /**
   * Writes a list of all ingestible archives to the response.
   *
   * @param receiver JSON receiver
   * @param format   specifies the format of the response.  Currently only JSON is supported.
   * @throws IOException
   */
  void read(ResponseReceiver receiver, MetadataFormat format) throws IOException;

  /**
   * Returns the zip archive in the ingest directory corresponding to the given article.
   *
   * @param filename the filename of an ingestible archive
   * @return File pointing to the .zip archive
   * @throws java.io.FileNotFoundException if the archive does not exist
   */
  File getIngestibleArchive(String filename) throws IOException;

  /**
   * Moves the archive from the ingest directory to the ingested directory. Should be called immediately after an
   * archive is ingested from the ingest directory.
   *
   * @param filename the filename of the archive that was just ingested
   * @return the File in the ingested directory where the archive was moved to
   * @throws IOException
   */
  File archiveIngested(String filename) throws IOException;
}
