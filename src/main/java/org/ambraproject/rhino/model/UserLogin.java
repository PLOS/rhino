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
 * Class for tracking each time a user logs in
 *
 * @author Alex Kudlick 2/9/12
 */
public class UserLogin extends AmbraEntity {

  private Long userProfileID;
  private String sessionId;
  private String IP;
  private String userAgent;

  public UserLogin() {
    super();
  }

  public UserLogin(String sessionId, String IP, String userAgent) {
    this();
    this.sessionId = sessionId;
    this.IP = IP;
    this.userAgent = userAgent;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getIP() {
    return IP;
  }

  public void setIP(String IP) {
    this.IP = IP;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public Long getUserProfileID() {
    return userProfileID;
  }

  public void setUserProfileID(Long userProfileID) {
    this.userProfileID = userProfileID;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof UserLogin)) return false;

    UserLogin userLogin = (UserLogin) o;

    if (IP != null ? !IP.equals(userLogin.IP) : userLogin.IP != null) return false;
    if (sessionId != null ? !sessionId.equals(userLogin.sessionId) : userLogin.sessionId != null) return false;
    if (userAgent != null ? !userAgent.equals(userLogin.userAgent) : userLogin.userAgent != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = sessionId != null ? sessionId.hashCode() : 0;
    result = 31 * result + (IP != null ? IP.hashCode() : 0);
    result = 31 * result + (userAgent != null ? userAgent.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "UserLogin{" +
        "created='" + getCreated() + '\'' +
        "sessionId='" + sessionId + '\'' +
        ", IP='" + IP + '\'' +
        ", userAgent='" + userAgent + '\'' +
        '}';
  }
}
