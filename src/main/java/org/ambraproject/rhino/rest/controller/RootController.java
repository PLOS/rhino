package org.ambraproject.rhino.rest.controller;

import com.mangofactory.swagger.annotations.ApiIgnore;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class RootController {

  /**
   * Serve the root page to a browser.
   */
  @ApiIgnore
  @Transactional(readOnly = true)
  @RequestMapping(value = "/")
  public String rootPage() {
    return "index";
  }
}
