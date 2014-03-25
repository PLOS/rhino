package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class ConfigurationReadController extends RestController {

  @Autowired
  private ConfigurationReadService configurationReadService;

  @RequestMapping(value = "/config", method = RequestMethod.GET)
  public void readConfig(HttpServletRequest request, HttpServletResponse response,
                         @RequestParam(value = JSONP_CALLBACK_PARAM, required = false) String jsonp,
                         @RequestHeader(value = ACCEPT_REQUEST_HEADER, required = false) String accept)
      throws IOException {
    configurationReadService.read().respond(request, response, entityGson);
  }

  @RequestMapping(value = "/build", method = RequestMethod.GET)
  public void readBuild(HttpServletRequest request, HttpServletResponse response,
                        @RequestParam(value = JSONP_CALLBACK_PARAM, required = false) String jsonp,
                        @RequestHeader(value = ACCEPT_REQUEST_HEADER, required = false) String accept)
      throws IOException {
    configurationReadService.readBuildProperties().respond(request, response, entityGson);
  }

}
