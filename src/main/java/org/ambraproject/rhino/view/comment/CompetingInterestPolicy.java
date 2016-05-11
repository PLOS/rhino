package org.ambraproject.rhino.view.comment;

import com.google.common.base.Strings;
import org.ambraproject.models.Annotation;
import org.ambraproject.rhino.config.RuntimeConfiguration;

import java.time.ZoneId;
import java.util.Date;

/**
 * Factory object for {@link CompetingInterestStatement} objects.
 */
class CompetingInterestPolicy {

  private final Date startDate;

  CompetingInterestPolicy(RuntimeConfiguration runtimeConfiguration) {
    this.startDate = Date.from(runtimeConfiguration.getCompetingInterestPolicyStart()
        .atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  CompetingInterestStatement createStatement(Annotation comment) {
    String competingInterestBody = comment.getCompetingInterestBody();
    boolean hasCompetingInterests = !Strings.isNullOrEmpty(competingInterestBody);
    boolean creatorWasPrompted = hasCompetingInterests || comment.getCreated().after(startDate);
    return !creatorWasPrompted ? NOT_PROMPTED : !hasCompetingInterests ? NO_STATEMENT
        : new CompetingInterestStatement(true, true, competingInterestBody);
  }

  private static final CompetingInterestStatement NOT_PROMPTED = new CompetingInterestStatement(false, false, null);
  private static final CompetingInterestStatement NO_STATEMENT = new CompetingInterestStatement(true, false, null);

}
