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
import org.ambraproject.rhino.BaseRhinoTest;
import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.util.response.Transceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class UserCrudServiceTest extends BaseRhinoTest {
  @Autowired
  private UserCrudService userCrudService;

  @Test
  public void testListUsers() throws Exception {
    UserProfile up1 = new UserProfile();
    up1.setAuthId("12345");
    up1.setDisplayName("displayname1");
    up1.setEmail("test1@blah.blah");
    up1.setPassword("test1");
    hibernateTemplate.save(up1);

    UserProfile up2 = new UserProfile();
    up2.setAuthId("23456");
    up2.setDisplayName("displayname2");
    up2.setEmail("test2@blah.blah");
    up2.setPassword("test2");
    hibernateTemplate.save(up2);

    Transceiver response = userCrudService.listUsers();
    Map<?, ?> userList = entityGson.fromJson(response.readJson(entityGson), Map.class);

    // there are other users in the database, just check to see if the ones we added are there or not.
    assertTrue(userList.containsKey("12345"));
    assertTrue(userList.containsKey("23456"));
  }

}
