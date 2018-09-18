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

package org.ambraproject.rhino.model.ingest;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.ambraproject.rhino.rest.RestClientException;
import org.springframework.http.HttpStatus;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

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

  REVIEW_LETTER {
    private final ImmutableSet<FileType> TYPES = Sets.immutableEnumSet(FileType.LETTER);

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
    @Override
    protected ImmutableSet<FileType> getSupportedFileTypes() {
      return STANDARD_THUMBNAIL_FILE_TYPES;
    }
  };

  // Shared by FIGURE and TABLE
  private static final ImmutableSet<FileType> STANDARD_THUMBNAIL_FILE_TYPES = Sets.immutableEnumSet(
      FileType.ORIGINAL, FileType.SMALL, FileType.INLINE, FileType.MEDIUM, FileType.LARGE);

  private final String identifier = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());

  public String getIdentifier() {
    return identifier;
  }

  FileType getFileType(String fileTypeIdentifier) {
    FileType fileType = FileType.fromIdentifier(fileTypeIdentifier).orElseThrow(() ->
        new RestClientException("File type not recognized: " + fileTypeIdentifier, HttpStatus.BAD_REQUEST));
    if (!getSupportedFileTypes().contains(fileType)) {
      String message = String.format("File type is not valid for %s: %s", this.identifier, fileTypeIdentifier);
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

  private static final ImmutableMap<String, AssetType> BY_IDENTIFIER = Maps.uniqueIndex(EnumSet.allOf(AssetType.class),
      input -> input.identifier);

  public static Optional<AssetType> fromIdentifier(String identifier) {
    return Optional.ofNullable(BY_IDENTIFIER.get(Objects.requireNonNull(identifier)));
  }

}
