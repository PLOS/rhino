package org.ambraproject.rhino.rest.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class RootController {

  /**
   * Serve the root page to a browser.
   */
  @RequestMapping(value = "/")
  public String rootPage() {
    return "root";
  }

}
