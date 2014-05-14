package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

@Controller
public class ContentRepoController extends RestController {

  @Autowired
  private RuntimeConfiguration runtimeConfiguration;

  @RequestMapping("repo/{key}/{version}")
  public ResponseEntity<?> serveFromRepo(@PathVariable("key") String key,
                                         @PathVariable("version") String version)
      throws IOException {
    File devModeRepo = runtimeConfiguration.devModeRepo();
    if (devModeRepo == null) {
      throw new RuntimeException("No devModeRepo supplied; non-dev-mode is not supported yet");
    }
    return serveInDevMode(devModeRepo, key);
  }

  private static ResponseEntity<String> serveInDevMode(File devModeRepo, String key) throws IOException {
    File path = new File(devModeRepo, key);
    if (!path.exists()) {
      return respondWithStatus(HttpStatus.NOT_FOUND);
    }

    String responseBody;
    try (Reader reader = new FileReader(path)) {
      responseBody = IOUtils.toString(reader);
    }
    return new ResponseEntity<>(responseBody, HttpStatus.OK);
  }

}
