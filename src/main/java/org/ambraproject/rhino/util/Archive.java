/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import org.plos.crepo.model.input.RepoObjectInput;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Abstraction over a .zip archive or equivalent structure.
 */
public abstract class Archive implements Closeable {

  private final String archiveName;

  /**
   * Keys are zip file entry names. Values are objects from which the file content may be extracted. Subclasses define
   * the value type. (This would be a good thing to make a generic type parameter, except that we don't want to expose
   * it publicly.)
   */
  private final ImmutableMap<String, ?> files;

  private Archive(String archiveName, Map<String, ?> files) {
    this.archiveName = Preconditions.checkNotNull(archiveName);
    this.files = ImmutableMap.copyOf(files);
  }

  /**
   * Return the name of a zip archive file representing this archive.
   *
   * @return the zip archive file name
   */
  public final String getArchiveName() {
    return archiveName;
  }

  protected final ImmutableMap<String, ?> getFiles() {
    return files;
  }

  /**
   * Return the set of file entry names in this archive.
   *
   * @return the set of file entry names
   */
  public final ImmutableSet<String> getEntryNames() {
    return files.keySet();
  }

  /**
   * Open a file from the archive. The argument must be one of the strings contained in the set returned by {@link
   * #getEntryNames()}.
   * <p/>
   * Must not be called if {@link #close()} has been called on this object. Behavior is undefined in this case.
   *
   * @param entryName the name of a file entry
   * @return a stream containing the file
   * @throws IllegalArgumentException if no entry with that name is in the archive
   */
  public final InputStream openFile(String entryName) {
    Object fileObj = files.get(Objects.requireNonNull(entryName));
    if (fileObj == null) throw new IllegalArgumentException();
    return openFileFrom(fileObj);
  }

  protected abstract InputStream openFileFrom(Object fileObj);

  public final RepoObjectInput.ContentAccessor getContentAccessorFor(final String entryName) {
    if (!files.containsKey(Preconditions.checkNotNull(entryName))) {
      throw new IllegalArgumentException("Archive does not contain an entry named: " + entryName);
    }
    return () -> openFile(entryName);
  }

  /**
   * Release or delete resources associated with storing the archive contents. Files cannot be opened afterward.
   */
  @Override
  public void close() {
  }

  public final void write(OutputStream stream) throws IOException {
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(stream)) {
      for (Map.Entry<String, ?> entry : files.entrySet()) {
        zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
        try (InputStream entryFile = openFileFrom(entry.getValue())) {
          ByteStreams.copy(entryFile, zipOutputStream);
        }
      }
    } finally {
      stream.close();
    }
  }


  public static Archive readZipFile(File file) throws IOException {
    try (InputStream stream = new FileInputStream(file)) {
      return readZipFile(file.getName(), stream);
    }
  }

  /**
   * Read a zip file from a stream to temp files on disk. Creating the {@code Archive} object exhausts the stream.
   * Closing the archive deletes the temp files.
   *
   * ZipStream.getNextEntry() will return an additional ZipEntry for directories, including archive files.
   * Nested asset ingestion is not supported in Rhino and these ZipEntries are skipped during repackaging.
   *
   * @param zipFile a stream containing the zip archive
   * @return the archive representing the read files
   * @throws IOException
   */
  public static Archive readZipFile(String archiveName, InputStream zipFile) throws IOException {
    ImmutableMap.Builder<String, File> tempFiles = ImmutableMap.builder();
    try (ZipInputStream zipStream = new ZipInputStream(zipFile)) {
      String prefix = "archive_" + new Date().getTime() + "_";

      ZipEntry entry;
      while ((entry = zipStream.getNextEntry()) != null) {

        if (entry.isDirectory()) {
          continue;
        }

        File tempFile = File.createTempFile(prefix, null);
        try (OutputStream tempFileStream = new FileOutputStream(tempFile)) {
          ByteStreams.copy(zipStream, tempFileStream);
        }
        tempFiles.put(entry.getName(), tempFile);
      }
    } finally {
      zipFile.close();
    }

    return new Archive(archiveName, tempFiles.build()) {
      @Override
      protected InputStream openFileFrom(Object file) {
        try {
          return new FileInputStream((File) file);
        } catch (FileNotFoundException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void close() {
        for (Object file : getFiles().values()) {
          ((File) file).delete();
        }
      }
    };
  }

  public static Archive readZipFileIntoMemory(File file) throws IOException {
    try (InputStream stream = new FileInputStream(file)) {
      return readZipFileIntoMemory(file.getName(), stream);
    }
  }

  public static Archive readZipFileIntoMemory(String archiveName, InputStream zipFile) throws IOException {
    ImmutableMap.Builder<String, byte[]> files = ImmutableMap.builder();
    try (ZipInputStream zipStream = new ZipInputStream(zipFile)) {
      ZipEntry entry;
      while ((entry = zipStream.getNextEntry()) != null) {
        byte[] fileContent = ByteStreams.toByteArray(zipStream);
        files.put(entry.getName(), fileContent);
      }
    } finally {
      zipFile.close();
    }

    return new Archive(archiveName, files.build()) {
      @Override
      protected InputStream openFileFrom(Object fileContent) {
        return new ByteArrayInputStream((byte[]) fileContent);
      }
    };
  }

  public static Archive pack(String archiveName, Map<String, ? extends ByteSource> files) {
    final ImmutableMap<String, ByteSource> defensiveFiles = ImmutableMap.copyOf(files);
    return new Archive(archiveName, defensiveFiles) {
      @Override
      protected InputStream openFileFrom(Object source) {
        try {
          return ((ByteSource) source).openStream();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

}
