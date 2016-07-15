package org.ambraproject.rhino.view.journal;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class IssueInputView {

  // Immutable by convention, but non-final to allow Gson's reflection magic
  private String doi;
  private String displayName;
  private String imageDoi;
  private List<String> articleOrder;

  public String getDoi() {
    return doi;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getImageDoi() {
    return imageDoi;
  }

  public List<String> getArticleOrder() {
    return (articleOrder == null) ? null : (articleOrder = ImmutableList.copyOf(articleOrder));
  }

}
