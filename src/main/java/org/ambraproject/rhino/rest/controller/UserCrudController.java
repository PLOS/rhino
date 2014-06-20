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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Controller for users
 */
@Controller
public class UserCrudController extends RestController {

  private static final String USER_ROOT = "/users";
  private static final String USER_TEMPLATE = USER_ROOT + "/{authId}";

  @Autowired
  private UserCrudService userCrudService;

  @Transactional(readOnly = true)
  @RequestMapping(value = USER_ROOT, method = RequestMethod.GET)
  public void listUsers(HttpServletRequest request, HttpServletResponse response) throws IOException {
    userCrudService.listUsers().respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = USER_TEMPLATE, method = RequestMethod.GET)
  public void read(HttpServletRequest request, HttpServletResponse response, @PathVariable String authId) throws IOException {
    userCrudService.read(authId).respond(request, response, entityGson);
  }

  @Transactional(rollbackFor = {Throwable.class})
  @RequestMapping(value = USER_TEMPLATE, method = RequestMethod.POST)
  public void createUserLogin(HttpServletRequest request, HttpServletResponse response, @PathVariable String authId) throws IOException {

    UserLogin input = readJsonFromRequest(request, UserLogin.class);
    userCrudService.createUserLogin(authId, input);

    userCrudService.read(authId).respond(request, response, entityGson);
  }

}
