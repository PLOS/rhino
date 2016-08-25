package org.ambraproject.rhino.model.ingest;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.ambraproject.rhino.rest.RestClientException;
import org.springframework.http.HttpStatus;

/**
 * The type of an asset (with a unique asset DOI, represented by one or more files) as determined by where the asset DOI
 * is referenced in the manuscript of its parent article.
 */
public enum AssetType {

  ARTICLE {
    private final ImmutableSet<FileType> TYPES = Sets.immutableEnumSet(FileType.MANUSCRIPT, FileType.PRINTABLE);
    private final ImmutableSet<FileType> REQUIRED = Sets.immutableEnumSet(FileType.MANUSCRIPT);

    @Override
    protected ImmutableSet<FileType> getSupportedFileTypes() {
      return TYPES;
    }

    @Override
    protected ImmutableSet<FileType> getRequiredFileTypes() {
      return REQUIRED;
    }
  },

  FIGURE {
    @Override
    protected ImmutableSet<FileType> getSupportedFileTypes() {
      return STANDARD_THUMBNAIL_FILE_TYPES;
    }
  },

  TABLE {
    @Override
    protected ImmutableSet<FileType> getSupportedFileTypes() {
      return STANDARD_THUMBNAIL_FILE_TYPES;
    }
  },

  GRAPHIC {
    private final ImmutableSet<FileType> TYPES = Sets.immutableEnumSet(FileType.ORIGINAL, FileType.THUMBNAIL);

    @Override
    protected ImmutableSet<FileType> getSupportedFileTypes() {
      return TYPES;
    }
  },

  SUPPLEMENTARY_MATERIAL {
    private final ImmutableSet<FileType> TYPES = Sets.immutableEnumSet(FileType.SUPPLEMENTARY);

    @Override
    protected ImmutableSet<FileType> getSupportedFileTypes() {
      return TYPES;
    }
  },

  STANDALONE_STRIKING_IMAGE {
    private final ImmutableSet<FileType> TYPES = Sets.immutableEnumSet(FileType.STRIKING_IMAGE);

    @Override
    protected ImmutableSet<FileType> getSupportedFileTypes() {
      return TYPES;
    }
  };

  // Shared by FIGURE and TABLE
  private static final ImmutableSet<FileType> STANDARD_THUMBNAIL_FILE_TYPES = Sets.immutableEnumSet(
      FileType.ORIGINAL, FileType.SMALL, FileType.INLINE, FileType.MEDIUM, FileType.LARGE);

  private final String identifier = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());

  public String getIdentifier() {
    return identifier;
  }

  FileType getFileType(String identifier) {
    FileType fileType = FileType.fromIdentifier(identifier).orElseThrow(() ->
        new RestClientException("File type not recognized: " + identifier, HttpStatus.BAD_REQUEST));
    if (!getSupportedFileTypes().contains(fileType)) {
      String message = String.format("File type not recognized: %s", identifier);
      throw new RestClientException(message, HttpStatus.BAD_REQUEST);
    }
    return fileType;
  }

  /**
   * Return the set of file types that this asset type may return from {@link #getFileType}.
   */
  protected abstract ImmutableSet<FileType> getSupportedFileTypes();

  /**
   * Return the set of file types that an ingestible must supply for each asset of this type. Must be a subset of {@link
   * #getSupportedFileTypes}.
   */
  protected ImmutableSet<FileType> getRequiredFileTypes() {
    return getSupportedFileTypes();
  }

}
