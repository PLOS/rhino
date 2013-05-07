package org.ambraproject.rhino.view.journal;

public class VolumeInputView {

  // Immutable by convention, but non-final to allow Gson's reflection magic
  private String volumeUri;
  private String displayName;
  private String imageUri;

  public String getVolumeUri() {
    return volumeUri;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getImageUri() {
    return imageUri;
  }

}
