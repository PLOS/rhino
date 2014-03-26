package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class ConfigurationReadController extends RestController {

  @Autowired
  private ConfigurationReadService configurationReadService;

  @Transactional(readOnly = true)
  @RequestMapping(value = "/config", method = RequestMethod.GET)
  public void readConfig(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    configurationReadService.read().respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = "/build", method = RequestMethod.GET)
  public void readBuild(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    configurationReadService.readBuildProperties().respond(request, response, entityGson);
  }

}
