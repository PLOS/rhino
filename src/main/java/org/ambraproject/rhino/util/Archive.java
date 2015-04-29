package org.ambraproject.rhino.util;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import org.plos.crepo.model.RepoCollectionMetadata;
import org.plos.crepo.model.RepoObject;
import org.plos.crepo.model.RepoObjectMetadata;
import org.plos.crepo.model.RepoVersion;
import org.plos.crepo.service.ContentRepoService;

import javax.annotation.Nullable;
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

  Archive(String archiveName, Map<String, ?> files) {
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
    Object fileObj = files.get(entryName);
    if (fileObj == null) throw new IllegalArgumentException();
    return openFileFrom(fileObj);
  }

  protected abstract InputStream openFileFrom(Object fileObj);

  public final RepoObject.ContentAccessor getContentAccessorFor(final String entryName) {
    if (!files.containsKey(Preconditions.checkNotNull(entryName))) {
      throw new IllegalArgumentException("Archive does not contain an entry named: " + entryName);
    }
    return new RepoObject.ContentAccessor() {
      @Override
      public InputStream open() throws IOException {
        return openFile(entryName);
      }
    };
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

  public static final Function<RepoObjectMetadata, String> USE_DOWNLOAD_NAME = new Function<RepoObjectMetadata, String>() {
    @Nullable
    @Override
    public String apply(RepoObjectMetadata input) {
      return input.getDownloadName().orNull();
    }
  };

  /**
   * Represent an archive from a content repo collection, equivalent to the actual zip archive that would have been
   * ingested to create the collection.
   *
   * @param service            a content repo service that can be used to read objects in the collection
   * @param archiveName        the name of the .zip file being represented
   * @param collection         the collection version
   * @param entryNameExtractor logic for recovering a zip entry name from a content repo object (return null to exclude
   *                           the object from the archive)
   * @return the archive representation
   */
  public static Archive readCollection(final ContentRepoService service, String archiveName,
                                       RepoCollectionMetadata collection,
                                       Function<? super RepoObjectMetadata, String> entryNameExtractor) {
    Preconditions.checkNotNull(service);

    ImmutableMap.Builder<String, RepoVersion> objects = ImmutableMap.builder();
    for (RepoObjectMetadata objectMetadata : collection.getObjects()) {
      RepoVersion version = objectMetadata.getVersion();

      String entryName = entryNameExtractor.apply(objectMetadata);
      if (entryName != null) {
        objects.put(entryName, version);
      }
    }

    return new Archive(archiveName, objects.build()) {
      @Override
      protected InputStream openFileFrom(Object repoVersion) {
        return service.getRepoObject((RepoVersion) repoVersion);
      }
    };
  }

}
