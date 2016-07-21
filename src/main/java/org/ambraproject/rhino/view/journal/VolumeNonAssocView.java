package org.ambraproject.rhino.view.journal;

import org.ambraproject.rhino.model.Volume;

/**
 * Simple view for {@link org.ambraproject.models.Volume}'s non-associative fields.
 */
public class VolumeNonAssocView {

  private final String doi;
  private final String displayName;
  private final String imageArticleDoi;

  public VolumeNonAssocView(String doi,
                            String displayName,
                            String imageArticleDoi) {
    this.doi = doi;
    this.displayName = displayName;
    this.imageArticleDoi = imageArticleDoi;
  }

  public VolumeNonAssocView(Volume volume){
    this(volume.getDoi(),
         volume.getDisplayName(),
         volume.getImageArticle().getDoi());
  }

}
