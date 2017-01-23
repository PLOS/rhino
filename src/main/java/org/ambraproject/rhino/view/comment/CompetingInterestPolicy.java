/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.view.comment;

import com.google.common.base.Strings;
import org.ambraproject.rhino.model.Comment;
import org.ambraproject.rhino.config.RuntimeConfiguration;

import java.time.ZoneId;
import java.util.Date;

/**
 * Factory object for {@link CompetingInterestStatement} objects.
 */
public class CompetingInterestPolicy {

  private final Date startDate;

  public CompetingInterestPolicy(RuntimeConfiguration runtimeConfiguration) {
    this.startDate = Date.from(runtimeConfiguration.getCompetingInterestPolicyStart()
        .atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  CompetingInterestStatement createStatement(Comment comment) {
    String competingInterestBody = comment.getCompetingInterestBody();
    boolean hasCompetingInterests = !Strings.isNullOrEmpty(competingInterestBody);
    boolean creatorWasPrompted = hasCompetingInterests || comment.getCreated().after(startDate);
    return !creatorWasPrompted ? NOT_PROMPTED : !hasCompetingInterests ? NO_STATEMENT
        : new CompetingInterestStatement(true, true, competingInterestBody);
  }

  private static final CompetingInterestStatement NOT_PROMPTED = new CompetingInterestStatement(false, false, null);
  private static final CompetingInterestStatement NO_STATEMENT = new CompetingInterestStatement(true, false, null);

}
