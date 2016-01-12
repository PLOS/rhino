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

package org.ambraproject.rhino.rest.controller;

import org.ambraproject.models.UserLogin;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.UserCrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controller for users
 */
@Controller
public class UserCrudController extends RestController {

  private static final String USER_ROOT = "/user";

  @Autowired
  private UserCrudService userCrudService;

  @Transactional(readOnly = true)
  @RequestMapping(value = USER_ROOT, method = RequestMethod.GET, params = "authId")
  public void readUsingAuthId(HttpServletRequest request, HttpServletResponse response, @RequestParam String authId) throws IOException {
    userCrudService.readUsingAuthId(authId).respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = USER_ROOT, method = RequestMethod.GET, params = "displayName")
  public void readUsingDisplayName(HttpServletRequest request, HttpServletResponse response,
                                   @RequestParam String displayName) throws IOException {
    userCrudService.readUsingDisplayName(displayName).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = USER_ROOT, method = RequestMethod.POST)
  public void createUserLogin(HttpServletRequest request, HttpServletResponse response, @RequestParam String authId) throws IOException {

    UserLogin input = readJsonFromRequest(request, UserLogin.class);
    userCrudService.createUserLogin(authId, input);

    userCrudService.readUsingAuthId(authId).respond(request, response, entityGson);
  }

}
