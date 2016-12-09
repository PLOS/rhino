package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.config.RuntimeConfiguration;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;

@Controller
public class ContentRepoController extends RestController {

  @Autowired
  private RuntimeConfiguration runtimeConfiguration;

  @RequestMapping(value = "repo/{key}/{version}", method = RequestMethod.GET)
  public ResponseEntity<?> serve(@PathVariable("key") String key,
                                 @PathVariable("version") String version)
      throws IOException {
    RuntimeConfiguration.ContentRepoEndpoint editorialBucket = runtimeConfiguration.getEditorialStorage();
    URI address;
    String bucketName;
    if (editorialBucket == null
        || (address = editorialBucket.getAddress()) == null
        || (bucketName = editorialBucket.getDefaultBucket()) == null) {
      throw new RuntimeException("contentRepo.editorial is not configured");
    }
    if ("file".equals(address.getScheme())) {
      return serveInDevMode(address, key, version);
    }

    return serveFromRemoteRepo(address, bucketName, key, version);


  }

  private static ResponseEntity<?> serveFromRemoteRepo(URI contentRepoAddress,
                                                       String bucket, String key, String version) {
    URI location = URI.create(String.format("%s/objects/%s?key=%s", // TODO Use version
        contentRepoAddress, bucket, key));
    HttpHeaders headers = new HttpHeaders();
    headers.setLocation(location);
    return new ResponseEntity<Object>(headers, HttpStatus.FOUND);
  }

  private static ResponseEntity<String> serveInDevMode(URI devModeRepo, String key, String version)
      throws IOException {
    File path = new File(devModeRepo.getPath(), key);
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
