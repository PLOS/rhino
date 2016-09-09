package org.ambraproject.rhino.model.ingest;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

/**
 * The type of a file that represents an asset. Each asset type has a set of these that it uses, as defined by {@link
 * AssetType#getSupportedFileTypes}. File types may be shared among more than one asset type.
 */
public enum FileType {

  // Root-level files that belong to the article itself
  MANUSCRIPT, PRINTABLE,

  // The source representation of an image
  ORIGINAL,

  // The single display format for an inline graphic or similar asset that doesn't have different thumbnail sizes
  THUMBNAIL,

  // Display formats at different sizes for figures and tables
  SMALL, MEDIUM, INLINE, LARGE,

  // A supplementary information file, which should always be the only file with its DOI
  SUPPLEMENTARY;

  private final String identifier = CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name());

  public String getIdentifier() {
    return identifier;
  }

  private static final ImmutableMap<String, FileType> BY_IDENTIFIER = Maps.uniqueIndex(EnumSet.allOf(FileType.class),
      input -> input.identifier);

  public static Optional<FileType> fromIdentifier(String identifier) {
    return Optional.ofNullable(BY_IDENTIFIER.get(Objects.requireNonNull(identifier)));
  }

}
