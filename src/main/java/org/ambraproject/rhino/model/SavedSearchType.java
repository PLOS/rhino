/*
 * Copyright (c) 2007-2013 by Public Library of Science
 * http://plos.org
 * http://ambraproject.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ambraproject.rhino.model;

/**
 * Types of savedSearches
 *
 * @author Joe Osowski
 */
public enum SavedSearchType {
  USER_DEFINED("User Defined"),
  JOURNAL_ALERT("Journal Alert");

  private String string;

  private SavedSearchType(String string) {
    this.string = string;
  }

  @Override
  public String toString() {
    return this.string;
  }

  public static SavedSearchType fromString(String string) {
    if (USER_DEFINED.string.equals(string)) {
      return USER_DEFINED;
    } else if (JOURNAL_ALERT.string.equals(string)) {
      return JOURNAL_ALERT;
    } else {
      throw new IllegalArgumentException("Unknown saved search type: " + string);
    }
  }
}
