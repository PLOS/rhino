package org.ambraproject.rhino.view.journal;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class IssueInputView {

  // Immutable by convention, but non-final to allow Gson's reflection magic
  private String issueUri;
  private String displayName;
  private String imageUri;
  private List<String> articleOrder;

  public String getIssueUri() {
    return issueUri;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getImageUri() {
    return imageUri;
  }

  public List<String> getArticleOrder() {
    return (articleOrder == null) ? null : (articleOrder = ImmutableList.copyOf(articleOrder));
  }

}
