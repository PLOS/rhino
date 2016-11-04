package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.response.TransientServiceResponse;
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
    TransientServiceResponse response;
    switch (configType) {
      case "build":
        response = configurationReadService.readBuildConfig();
        break;
      case "repo":
        response = configurationReadService.readRepoConfig();
        break;
      default:
        throw new RestClientException("Invalid configuration type parameter. Options are: " +
            ConfigurationReadService.CONFIG_TYPES.toString(), HttpStatus.BAD_REQUEST);
    }
    return response.asJsonResponse(entityGson);
  }

  @RequestMapping(value = CONFIG_ROOT + "/userApi", method = RequestMethod.GET)
  public ResponseEntity<?> readUserApiConfig() throws IOException {
    return TransientServiceResponse.serveView(runtimeConfiguration.getNedConfiguration()).asJsonResponse(entityGson);
  }

}
