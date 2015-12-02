package org.ambraproject.rhino.view.journal;

import org.ambraproject.models.Volume;

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

  public VolumeNonAssocView(Volume volume){
    this(volume.getVolumeUri(),
         volume.getDisplayName(),
         volume.getImageUri(),
         volume.getTitle(),
         volume.getDescription());
  }

}
