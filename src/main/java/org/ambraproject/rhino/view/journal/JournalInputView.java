package org.ambraproject.rhino.view.journal;

public class JournalInputView {

  // Immutable by convention, but non-final to allow Gson's reflection magic
  private String currentIssueUri;

  public String getCurrentIssueUri() {
    return currentIssueUri;
  }

}
