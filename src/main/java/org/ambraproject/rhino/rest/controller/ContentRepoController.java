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
    if (contentRepoAddress != null) {
      return "file".equals(contentRepoAddress.getScheme())
          ? serveInDevMode(contentRepoAddress, bucket, key, version)
          : serveFromRemoteRepo(contentRepoAddress, bucket, key, version);
    } else {
      throw new RuntimeException("contentRepoAddress required in configuration");
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

  private static ResponseEntity<String> serveInDevMode(URI devModeRepo, String bucket, String key, String version)
      throws IOException {
    File path = new File(devModeRepo.getPath(), bucket + '/' + key);
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
