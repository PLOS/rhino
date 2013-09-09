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
enum FigureType {

  ORIGINAL("TIF"),
  SMALL("PNG_S"),
  INLINE("PNG_I"),
  MEDIUM("PNG_M"),
  LARGE("PNG_L");

  private final ImmutableSet<String> associatedExtensions;

  private FigureType(String... associatedExtensions) {
    this.associatedExtensions = ImmutableSet.copyOf(associatedExtensions);
  }

  private static final ImmutableMap<String, FigureType> TYPES_BY_EXTENSION = buildExtensionMap();

  private static ImmutableMap<String, FigureType> buildExtensionMap() {
    ImmutableMap.Builder<String, FigureType> builder = ImmutableMap.builder();
    for (FigureType figureType : values()) {
      for (String extension : figureType.associatedExtensions) {
        builder.put(extension.toUpperCase(), figureType);
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

  static FigureType fromExtension(String extension) {
    FigureType figureType = TYPES_BY_EXTENSION.get(extension.toUpperCase());
    if (figureType == null) {
      String message = String.format("Figure extension not matched: \"%s\". Expected one of: %s",
          extension, getAllExtensions());
      throw new IllegalArgumentException(message);
    }
    return figureType;
  }

}
