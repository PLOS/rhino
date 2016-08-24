package org.ambraproject.rhino.model.ingest;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.ambraproject.rhino.rest.RestClientException;
import org.springframework.http.HttpStatus;

import java.util.EnumSet;

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

    @Override
    protected FileType getFileType(String reprName) {
      switch (reprName) {
        case "XML":
          return FileType.MANUSCRIPT;
        case "PDF":
          return FileType.PRINTABLE;
        default:
          throw unmatchedReprException(reprName);
      }
    }
  },

  FIGURE {
    @Override
    protected ImmutableSet<FileType> getSupportedFileTypes() {
      return STANDARD_THUMBNAIL_FILE_TYPES;
    }

    @Override
    protected FileType getFileType(String reprName) {
      return getFileTypeForStandardThumbnails(reprName);
    }
  },

  TABLE {
    @Override
    protected ImmutableSet<FileType> getSupportedFileTypes() {
      return STANDARD_THUMBNAIL_FILE_TYPES;
    }

    @Override
    protected FileType getFileType(String reprName) {
      return getFileTypeForStandardThumbnails(reprName);
    }
  },

  GRAPHIC {
    private final ImmutableSet<FileType> TYPES = Sets.immutableEnumSet(FileType.ORIGINAL, FileType.THUMBNAIL);

    @Override
    protected ImmutableSet<FileType> getSupportedFileTypes() {
      return TYPES;
    }

    @Override
    protected FileType getFileType(String reprName) {
      switch (reprName) {
        case "TIF":
        case "GIF":
          return FileType.ORIGINAL;
        case "PNG":
          return FileType.THUMBNAIL;
        default:
          throw unmatchedReprException(reprName);
      }
    }
  },

  SUPPLEMENTARY_MATERIAL {
    private final ImmutableSet<FileType> TYPES = Sets.immutableEnumSet(FileType.SUPPLEMENTARY);

    @Override
    protected ImmutableSet<FileType> getSupportedFileTypes() {
      return TYPES;
    }

    @Override
    protected FileType getFileType(String reprName) {
      // Accept all file types
      return FileType.SUPPLEMENTARY;
    }
  },

  STANDALONE_STRIKING_IMAGE {
    private final ImmutableSet<FileType> TYPES = Sets.immutableEnumSet(FileType.STRIKING_IMAGE);

    @Override
    protected ImmutableSet<FileType> getSupportedFileTypes() {
      return TYPES;
    }

    @Override
    protected FileType getFileType(String reprName) {
      // Accept all file types
      // TODO: Validate on expected image file types?
      return FileType.STRIKING_IMAGE;
    }
  };

  // Shared by FIGURE and TABLE
  private static final ImmutableSet<FileType> STANDARD_THUMBNAIL_FILE_TYPES = Sets.immutableEnumSet(
      FileType.ORIGINAL, FileType.SMALL, FileType.INLINE, FileType.MEDIUM, FileType.LARGE);

  private static FileType getFileTypeForStandardThumbnails(String reprName) {
    switch (reprName) {
      case "TIF":
      case "TIFF":
        return FileType.ORIGINAL;
      case "PNG_S":
        return FileType.SMALL;
      case "PNG_I":
        return FileType.INLINE;
      case "PNG_M":
        return FileType.MEDIUM;
      case "PNG_L":
        return FileType.LARGE;
      default:
        throw unmatchedReprException(reprName);
    }
  }

  private final String identifier = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());

  public String getIdentifier() {
    return identifier;
  }

  /**
   * Match a repr name (as used in {@link org.ambraproject.rhino.content.xml.ManifestXml}) to a file type as defined for
   * this asset type. Must be one of the values contained in the set that this asset type returns for {@link
   * #getSupportedFileTypes}.
   *
   * @param reprName an uppercase repr name from a manifest file
   * @return the file type that will represent the file in JSON article metadata
   * @throws RestClientException if {@code reprName} is not matched to a file type for this asset type
   */
  protected abstract FileType getFileType(String reprName);

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

  private static final ImmutableMap<String, AssetType> BY_IDENTIFIER = Maps.uniqueIndex(EnumSet.allOf(AssetType.class),
      input -> input.identifier);

  private static RuntimeException unmatchedReprException(String reprName) {
    String message = "Manifest contains unmatched \"repr\" value: " + reprName;
    return new RestClientException(message, HttpStatus.BAD_REQUEST);
  }

}
