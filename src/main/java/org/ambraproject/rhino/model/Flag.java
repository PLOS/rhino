/*
 * $HeadURL$
 * $Id$
 * Copyright (c) 2006-2012 by Public Library of Science http://plos.org http://ambraproject.org
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.model;

/**
 * @author Alex Kudlick 3/7/12
 */
public class Flag extends AmbraEntity {

  private String comment;
  private Long userProfileID;
  private FlagReasonCode reason;
  private Annotation flaggedAnnotation;

  public Flag() {
    super();
    this.reason = FlagReasonCode.OTHER;
  }

  public Flag(Long userProfileID, FlagReasonCode reason, Annotation flaggedAnnotation) {
    super();
    this.userProfileID = userProfileID;
    this.reason = reason;
    this.flaggedAnnotation = flaggedAnnotation;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public Long getUserProfileID() {
    return userProfileID;
  }

  public void setUserProfileID(Long userProfileID) {
    this.userProfileID = userProfileID;
  }

  public FlagReasonCode getReason() {
    return reason;
  }

  public void setReason(FlagReasonCode reason) {
    this.reason = reason;
  }

  public Annotation getFlaggedAnnotation() {
    return flaggedAnnotation;
  }

  public void setFlaggedAnnotation(Annotation flaggedAnnotation) {
    this.flaggedAnnotation = flaggedAnnotation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Flag)) return false;

    Flag flag = (Flag) o;

    if (getID() != null ? !getID().equals(flag.getID()) : flag.getID() != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return getID().hashCode();
  }

  @Override
  public String toString() {
    return "Flag{" +
        "comment='" + comment + '\'' +
        ", reason=" + reason +
        '}';
  }
}
