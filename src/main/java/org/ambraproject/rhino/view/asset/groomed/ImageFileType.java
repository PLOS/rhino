package org.ambraproject.rhino.view.asset.groomed;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * Classifications for the asset files representing an image with thumbnails.
 * <p/>
 * Currently, files are associated with figure types strictly according to their file extensions, using conventions
 * pre-established by Ambra. As token future-proofing, this class allows multiple extensions to be mapped to a figure
 * type. But a better solution would be to have this explicitly declared as part of the ingestion input.
 */
enum ImageFileType {

  // The original, highest-resolution copy. Expected to be present for all figures.
  ORIGINAL("TIF", "TIFF"),

  // Resized thumbnails for "normal" figures, which under PLOS's naming scheme have DOIs like *.g000 or *.t000
  SMALL("PNG_S"),
  INLINE("PNG_I"),
  MEDIUM("PNG_M"),
  LARGE("PNG_L"),

  // Resized image for inline graphics
  // For PLOS articles, this includes math expressions with DOIs like *.e000
  GRAPHIC("PNG");


  private final ImmutableSet<String> associatedExtensions;

  private ImageFileType(String... associatedExtensions) {
    this.associatedExtensions = ImmutableSet.copyOf(associatedExtensions);
  }

  private static final ImmutableMap<String, ImageFileType> TYPES_BY_EXTENSION = buildExtensionMap();

  private static ImmutableMap<String, ImageFileType> buildExtensionMap() {
    ImmutableMap.Builder<String, ImageFileType> builder = ImmutableMap.builder();
    for (ImageFileType imageFileType : values()) {
      for (String extension : imageFileType.associatedExtensions) {
        builder.put(extension.toUpperCase(), imageFileType);
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

  static ImageFileType fromExtension(String extension) {
    ImageFileType imageFileType = TYPES_BY_EXTENSION.get(extension.toUpperCase());
    if (imageFileType == null) {
      String message = String.format("Figure extension not matched: \"%s\". Expected one of: %s",
          extension, getAllExtensions());
      throw new UncategorizedAssetException(message);
    }
    return imageFileType;
  }

}
