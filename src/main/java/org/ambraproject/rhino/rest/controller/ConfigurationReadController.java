package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class ConfigurationReadController extends RestController {

  @Autowired
  private ConfigurationReadService configurationReadService;

  @RequestMapping(value = "/config", method = RequestMethod.GET)
  public void readConfig(HttpServletResponse response) throws IOException {
    configurationReadService.read(response, MetadataFormat.JSON);
  }

}
