package org.ambraproject.rhino.view.journal;

public class VolumeInputView {

  // Immutable by convention, but non-final to allow Gson's reflection magic
  private String doi;
  private String displayName;
  private String imageArticleDoi;

  public String getDoi() {
    return doi;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getImageArticleDoi() {
    return imageArticleDoi;
  }

}
