/*
 * Copyright (c) 2006-2014 by Public Library of Science
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

package org.ambraproject.rhino.service;

import org.ambraproject.models.UserLogin;
import org.ambraproject.models.UserProfile;
import org.ambraproject.rhino.util.response.Transceiver;

import java.io.IOException;

/**
 * Service that deals with users
 */
public interface UserCrudService {

  public abstract Transceiver readUsingAuthId(String authId) throws IOException;

  /**
   * Record user logging in
   * @param authId authId
   * @param loginInfo UserLogin object
   * @return UserProfile object
   */
  public abstract UserProfile createUserLogin(final String authId, final UserLogin loginInfo);

}
