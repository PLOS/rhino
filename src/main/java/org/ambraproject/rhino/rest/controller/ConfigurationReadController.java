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

package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.response.ServiceResponse;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;

@Controller
public class ConfigurationReadController extends RestController {

  private static final String CONFIG_ROOT = "/config";
  private static final String CONFIG_TYPE_PARAM = "type";

  @Autowired
  private ConfigurationReadService configurationReadService;
  @Autowired
  private RuntimeConfiguration runtimeConfiguration;

  /**
   * Retrieves configuration metadata according to the given type parameters.
   * @param configType String indicating the type of config values to be returned (should be one of {@link
 *                   ConfigurationReadService.CONFIG_TYPES})
   **/

  @Transactional(readOnly = true)
  @RequestMapping(value = CONFIG_ROOT, method = RequestMethod.GET)
  public ResponseEntity<?> readConfig(@RequestParam(value = CONFIG_TYPE_PARAM, required = true) String configType)
      throws IOException {
    ServiceResponse<?> response;
    switch (configType) {
      case "build":
        response = configurationReadService.readBuildConfig();
        break;
      case "repo":
        response = configurationReadService.readRepoConfig();
        break;
      case "run":
        response = configurationReadService.readRunInfo();
        break;
      default:
        throw new RestClientException("Invalid configuration type parameter. Options are: " +
            ConfigurationReadService.CONFIG_TYPES.toString(), HttpStatus.BAD_REQUEST);
    }
    return response.asJsonResponse(entityGson);
  }

  @RequestMapping(value = CONFIG_ROOT + "/userApi", method = RequestMethod.GET)
  public ResponseEntity<?> readUserApiConfig() throws IOException {
    return ServiceResponse.serveView(runtimeConfiguration.getNedConfiguration()).asJsonResponse(entityGson);
  }

}
