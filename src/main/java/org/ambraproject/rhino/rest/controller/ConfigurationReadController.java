package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.rest.MetadataFormat;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.ConfigurationReadService;
import org.ambraproject.rhino.util.response.ResponseReceiver;
import org.ambraproject.rhino.util.response.ServletResponseReceiver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class ConfigurationReadController extends RestController {

  @Autowired
  private ConfigurationReadService configurationReadService;

  @RequestMapping(value = "/config", method = RequestMethod.GET)
  public void readConfig(HttpServletResponse response,
                         @RequestParam(value = JSONP_CALLBACK_PARAM, required = false) String jsonp,
                         @RequestHeader(value = ACCEPT_REQUEST_HEADER, required = false) String accept)
      throws IOException {
    MetadataFormat metadataFormat = MetadataFormat.getFromAcceptHeader(accept);
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(jsonp, response);
    configurationReadService.read(receiver, metadataFormat);
  }

  @RequestMapping(value = "/build", method = RequestMethod.GET)
  public void readBuild(HttpServletResponse response,
                        @RequestParam(value = JSONP_CALLBACK_PARAM, required = false) String jsonp,
                        @RequestHeader(value = ACCEPT_REQUEST_HEADER, required = false) String accept)
      throws IOException {
    MetadataFormat metadataFormat = MetadataFormat.getFromAcceptHeader(accept);
    ResponseReceiver receiver = ServletResponseReceiver.createForJson(jsonp, response);
    configurationReadService.readBuildProperties(receiver, metadataFormat);
  }

}
