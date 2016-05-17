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
 * Class for tracking each time a user performs a search
 *
 * @author Joe Osowski 2/16/2012
 */
public class UserSearch extends AmbraEntity {

  private Long userProfileID;
  private String searchTerms;
  private String searchParams;

  public UserSearch() {
    super();
  }

  public UserSearch(Long userProfileID, String searchTerms, String searchParams) {
    this();
    this.userProfileID = userProfileID;
    this.searchTerms = searchTerms;
    this.searchParams = searchParams;
  }

  public Long getUserProfileID() {
    return userProfileID;
  }

  public void setUserProfileID(Long userProfileID) {
    this.userProfileID = userProfileID;
  }

  public String getSearchTerms() {
    return searchTerms;
  }

  public void setSearchTerms(String searchTerms) {
    this.searchTerms = searchTerms;
  }

  public String getSearchParams() {
    return searchParams;
  }

  public void setSearchParams(String searchParams) {
    this.searchParams = searchParams;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UserSearch that = (UserSearch) o;

    if (searchParams != null ? !searchParams.equals(that.searchParams) : that.searchParams != null) return false;
    if (searchTerms != null ? !searchTerms.equals(that.searchTerms) : that.searchTerms != null) return false;
    if (userProfileID != null ? !userProfileID.equals(that.userProfileID) : that.userProfileID != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = userProfileID != null ? userProfileID.hashCode() : 0;
    result = 31 * result + (searchTerms != null ? searchTerms.hashCode() : 0);
    result = 31 * result + (searchParams != null ? searchParams.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "UserSearch{" +
        "userProfileID=" + userProfileID +
        ", searchTerms='" + searchTerms + '\'' +
        ", searchParams='" + searchParams + '\'' +
        '}';
  }
}