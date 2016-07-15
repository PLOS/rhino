package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.rest.RestClientException;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.ambraproject.rhino.util.response.Transceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Calendar;

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
   *
   * @param request    HttpServletRequest
   * @param response   HttpServletResponse
   * @param configType String indicating the type of config values to be returned (should be one of {@link
   *                   ConfigurationReadService.CONFIG_TYPES})
   **/

  @Transactional(readOnly = true)
  @RequestMapping(value = CONFIG_ROOT, method = RequestMethod.GET)
  public void readConfig(HttpServletRequest request, HttpServletResponse response,
                         @RequestParam(value = CONFIG_TYPE_PARAM, required = true) String configType)
      throws IOException {
    if (!ConfigurationReadService.CONFIG_TYPES.contains(configType)) {
      throw new RestClientException("Invalid configuration type parameter. Options are: " +
          ConfigurationReadService.CONFIG_TYPES.toString(), HttpStatus.BAD_REQUEST);
    }

    if (configType.contentEquals("ambra")) {
      configurationReadService.readAmbraConfig().respond(request, response, entityGson);
    } else if (configType.contentEquals("build")) {
      configurationReadService.readBuildConfig().respond(request, response, entityGson);
    } else if (configType.contentEquals("repo")) {
      configurationReadService.readRepoConfig().respond(request, response, entityGson);
    }
  }

  @RequestMapping(value = CONFIG_ROOT + "/userApi", method = RequestMethod.GET)
  public void readUserApiConfig(HttpServletRequest request, HttpServletResponse response) throws IOException {
    new Transceiver() {
      @Override
      protected Object getData() throws IOException {
        return runtimeConfiguration.getNedConfiguration();
      }

      @Override
      protected Calendar getLastModifiedDate() throws IOException {
        return null;
      }
    }.respond(request, response, entityGson);
  }

}
