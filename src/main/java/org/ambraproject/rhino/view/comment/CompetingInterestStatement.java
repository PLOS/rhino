package org.ambraproject.rhino.view.comment;

import com.google.common.base.Preconditions;

public class CompetingInterestStatement {
  private final boolean creatorWasPrompted;
  private final boolean hasCompetingInterests;
  private final String body;

  CompetingInterestStatement(boolean creatorWasPrompted, boolean hasCompetingInterests, String body) {
    Preconditions.checkArgument(creatorWasPrompted || !hasCompetingInterests);
    Preconditions.checkArgument(hasCompetingInterests == (body != null));
    this.creatorWasPrompted = creatorWasPrompted;
    this.hasCompetingInterests = hasCompetingInterests;
    this.body = body;
  }

}
