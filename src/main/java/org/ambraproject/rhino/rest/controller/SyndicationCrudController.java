package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.service.SyndicationCrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Controller
public class SyndicationCrudController extends RestController {

  @Autowired
  private SyndicationCrudService syndicationService;


  @Transactional(readOnly = true)
  @RequestMapping(value = "/syndications", method = RequestMethod.GET)
  public ResponseEntity<?> getSyndications(@RequestHeader(value = HttpHeaders.IF_MODIFIED_SINCE, required = false) Date ifModifiedSince,
                                           @RequestParam("journal") String journalKey,
                                           @RequestParam("status") List<String> statuses)
      throws IOException {
    return syndicationService.readSyndications(journalKey, statuses).asJsonResponse(ifModifiedSince, entityGson);
  }
}
