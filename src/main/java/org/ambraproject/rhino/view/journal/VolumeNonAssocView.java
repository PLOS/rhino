package org.ambraproject.rhino.view.journal;

import com.google.common.base.Preconditions;

/**
 * Simple view for {@link org.ambraproject.models.Volume}'s non-associative fields.
 */
public class VolumeNonAssocView {

  private final String volumeUri;
  private final String displayName;
  private final String imageUri;
  private final String title;
  private final String description;

  public VolumeNonAssocView(String volumeUri,
                            String displayName,
                            String imageUri,
                            String title,
                            String description) {
    this.volumeUri = volumeUri;
    this.displayName = displayName;
    this.imageUri = imageUri;
    this.title = title;
    this.description = description;
  }

  /**
   * Convenience method for building from a SELECT'ed array of query results.
   */
  public static VolumeNonAssocView fromArray(Object[] array) {
    Preconditions.checkArgument(array.length == 5);
    return new VolumeNonAssocView(
        (String) array[0], (String) array[1], (String) array[2], (String) array[3], (String) array[4]);
  }

}
