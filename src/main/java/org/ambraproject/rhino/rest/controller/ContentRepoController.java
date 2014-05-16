package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;

@Controller
public class ContentRepoController extends RestController {

  @Autowired
  private RuntimeConfiguration runtimeConfiguration;

  @RequestMapping("repo/{bucket}/{key}/{version}")
  public ResponseEntity<?> serve(@PathVariable("bucket") String bucket,
                                 @PathVariable("key") String key,
                                 @PathVariable("version") String version)
      throws IOException {
    URI contentRepoAddress = runtimeConfiguration.getContentRepoAddress();
    File devModeRepo = runtimeConfiguration.getDevModeRepo();
    if (contentRepoAddress != null && devModeRepo != null) {
      String message = String.format("Both contentRepoAddress (%s) and devModeRepo (%s) are in configuration.",
          contentRepoAddress, devModeRepo);
      throw new RuntimeException(message);
    } else if (contentRepoAddress != null) {
      return serveFromRemoteRepo(contentRepoAddress, bucket, key, version);
    } else if (devModeRepo != null) {
      return serveInDevMode(devModeRepo, bucket, key);
    } else {
      throw new RuntimeException("contentRepoAddress or devModeRepo required in configuration");
    }
  }

  private static ResponseEntity<?> serveFromRemoteRepo(URI contentRepoAddress,
                                                       String bucket, String key, String version) {
    URI location = URI.create(String.format("%s/objects/%s?key=%s", // TODO Use version
        contentRepoAddress, bucket, key));
    HttpHeaders headers = new HttpHeaders();
    headers.setLocation(location);
    return new ResponseEntity<Object>(headers, HttpStatus.FOUND);
  }

  private static ResponseEntity<String> serveInDevMode(File devModeRepo, String bucket, String key)
      throws IOException {
    File path = new File(devModeRepo, bucket + '/' + key);
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
