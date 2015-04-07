package org.ambraproject.rhino.util;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.model.RepoVersion;
import org.plos.crepo.service.ContentRepoService;

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
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Abstraction over a .zip archive or equivalent structure.
 */
public abstract class Archive implements Closeable {
  Archive() {
  }

  /**
   * Return the name of a zip archive file representing this archive.
   *
   * @return the zip archive file name
   */
  public abstract String getArchiveName();

  /**
   * Return the set of file entry names in this archive.
   *
   * @return the set of file entry names
   */
  public abstract Set<String> getEntryNames();

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
  public abstract InputStream openFile(String entryName);

  /**
   * Release or delete resources associated with storing the archive contents. Files cannot be opened afterward.
   */
  @Override
  public abstract void close();


  public static Archive readZipFile(File file) throws IOException {
    try (InputStream stream = new FileInputStream(file)) {
      return readZipFile(file.getName(), stream);
    }
  }

  /**
   * Read a zip file from a stream to temp files on disk. Creating the {@code Archive} object exhausts the stream.
   * Closing the archive deletes the temp files.
   *
   * @param zipFile a stream containing the zip archive
   * @return the archive representing the read files
   * @throws IOException
   */
  public static Archive readZipFile(final String archiveName, InputStream zipFile) throws IOException {
    Preconditions.checkNotNull(archiveName);

    final ImmutableMap<String, File> tempFiles;
    try (ZipInputStream zipStream = new ZipInputStream(zipFile)) {
      String prefix = "archive_" + new Date().getTime() + "_";
      ImmutableMap.Builder<String, File> tempFilesBuilder = ImmutableMap.builder();

      ZipEntry entry;
      while ((entry = zipStream.getNextEntry()) != null) {
        File tempFile = File.createTempFile(prefix, null);
        try (OutputStream tempFileStream = new FileOutputStream(tempFile)) {
          ByteStreams.copy(zipStream, tempFileStream);
        }
        tempFilesBuilder.put(entry.getName(), tempFile);
      }

      tempFiles = tempFilesBuilder.build();
    } finally {
      zipFile.close();
    }

    return new Archive() {
      @Override
      public String getArchiveName() {
        return archiveName;
      }

      @Override
      public Set<String> getEntryNames() {
        return tempFiles.keySet();
      }

      @Override
      public InputStream openFile(String entryName) {
        File file = tempFiles.get(entryName);
        if (file == null) throw new IllegalArgumentException();
        try {
          return new FileInputStream(file);
        } catch (FileNotFoundException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void close() {
        for (File file : tempFiles.values()) {
          file.delete();
        }
      }
    };
  }

  public static Archive readZipFileIntoMemory(File file) throws IOException {
    try (InputStream stream = new FileInputStream(file)) {
      return readZipFileIntoMemory(file.getName(), stream);
    }
  }

  public static Archive readZipFileIntoMemory(final String archiveName, InputStream zipFile) throws IOException {
    Preconditions.checkNotNull(archiveName);

    final ImmutableMap<String, byte[]> files;
    try (ZipInputStream zipStream = new ZipInputStream(zipFile)) {
      ImmutableMap.Builder<String, byte[]> filesBuilder = ImmutableMap.builder();
      ZipEntry entry;
      while ((entry = zipStream.getNextEntry()) != null) {
        byte[] fileContent = ByteStreams.toByteArray(zipStream);
        filesBuilder.put(entry.getName(), fileContent);
      }
      files = filesBuilder.build();
    } finally {
      zipFile.close();
    }

    return new Archive() {
      @Override
      public String getArchiveName() {
        return archiveName;
      }

      @Override
      public Set<String> getEntryNames() {
        return files.keySet();
      }

      @Override
      public InputStream openFile(String entryName) {
        byte[] fileContent = files.get(entryName);
        if (fileContent == null) throw new IllegalArgumentException();
        return new ByteArrayInputStream(fileContent);
      }

      @Override
      public void close() {
      }
    };
  }

  public static Archive readCollection(final ContentRepoService service, final RepoCollectionMetadata collection) {
    String key = collection.getVersion().getKey();
    int slashIndex = key.lastIndexOf('/');
    String lastToken = (slashIndex < 0) ? key : key.substring(slashIndex + 1);
    String archiveName = lastToken + ".zip";

    return readCollection(archiveName, service, collection);
  }

  /**
   * Represent an archive from a content repo collection, equivalent to the actual zip archive that would have been
   * ingested to create the collection.
   *
   * @param service    a content repo service that can be used to read objects in the collection
   * @param collection the collection version
   * @return the archive representation
   */
  public static Archive readCollection(final String archiveName,
                                       final ContentRepoService service,
                                       final RepoCollectionMetadata collection) {
    Preconditions.checkNotNull(archiveName);
    Preconditions.checkNotNull(service);

    ImmutableMap.Builder<String, RepoVersion> objectsBuilder = ImmutableMap.builder();
    for (RepoObjectMetadata objectMetadata : collection.getObjects()) {
      Optional<String> downloadName = objectMetadata.getDownloadName();
      RepoVersion version = objectMetadata.getVersion();
      if (downloadName.isPresent()) {
        objectsBuilder.put(downloadName.get(), version);
      } else {
        String message = "Repo objects must have downloadNames to be represented as an Archive. Object does not: " + version;
        throw new RuntimeException(message);
      }
    }
    final ImmutableMap<String, RepoVersion> objects = objectsBuilder.build();

    return new Archive() {
      @Override
      public String getArchiveName() {
        return archiveName;
      }

      @Override
      public Set<String> getEntryNames() {
        return objects.keySet();
      }

      @Override
      public InputStream openFile(String entryName) {
        RepoVersion repoVersion = objects.get(entryName);
        if (repoVersion == null) throw new IllegalArgumentException();
        return service.getRepoObject(repoVersion);
      }

      @Override
      public void close() {
      }
    };
  }

}
