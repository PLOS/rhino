package org.ambraproject.rhino.view.asset.groomed;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Classifications for the asset files representing a figure.
 * <p/>
 * Currently, files are associated with figure types strictly according to their file extensions, using conventions
 * pre-established by Ambra. As token future-proofing, this class allows multiple extensions to be mapped to a figure
 * type. But a better solution would be to have this explicitly declared as part of the ingestion input.
 */
enum FigureFileType {

  ORIGINAL("TIF"),
  SMALL("PNG_S"),
  INLINE("PNG_I"),
  MEDIUM("PNG_M"),
  LARGE("PNG_L");

  private final ImmutableSet<String> associatedExtensions;

  private FigureFileType(String... associatedExtensions) {
    this.associatedExtensions = ImmutableSet.copyOf(associatedExtensions);
  }

  private static final ImmutableMap<String, FigureFileType> TYPES_BY_EXTENSION = buildExtensionMap();

  private static ImmutableMap<String, FigureFileType> buildExtensionMap() {
    ImmutableMap.Builder<String, FigureFileType> builder = ImmutableMap.builder();
    for (FigureFileType figureFileType : values()) {
      for (String extension : figureFileType.associatedExtensions) {
        builder.put(extension.toUpperCase(), figureFileType);
      }
    }
    return builder.build();
  }

  ImmutableSet<String> getAssociatedExtensions() {
    return associatedExtensions;
  }

  static ImmutableSet<String> getAllExtensions() {
    return TYPES_BY_EXTENSION.keySet();
  }

  static FigureFileType fromExtension(String extension) {
    FigureFileType figureFileType = TYPES_BY_EXTENSION.get(extension.toUpperCase());
    if (figureFileType == null) {
      String message = String.format("Figure extension not matched: \"%s\". Expected one of: %s",
          extension, getAllExtensions());
      throw new NotAFigureException(message);
    }
    return figureFileType;
  }

}
