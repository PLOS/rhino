package org.ambraproject.rhino.view.journal;

public class IssueInputView {

  // Immutable by convention, but non-final to allow Gson's reflection magic
  private String issueUri;
  private String displayName;
  private String imageUri;

  public String getIssueUri() {
    return issueUri;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getImageUri() {
    return imageUri;
  }

}
