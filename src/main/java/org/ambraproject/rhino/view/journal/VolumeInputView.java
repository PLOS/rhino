package org.ambraproject.rhino.view.journal;

import com.google.common.base.Strings;

public class VolumeInputView {

  // Immutable by convention, but non-final to allow Gson's reflection magic
  private String displayName;

  public String getDisplayName() {
    return Strings.nullToEmpty(displayName);
  }

}
