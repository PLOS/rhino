package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.service.SyndicationCrudService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Controller
public class SyndicationCrudController extends RestController {

  private static final Logger log = LoggerFactory.getLogger(SyndicationCrudController.class);

  private static final String JOURNAL_PARAM = "journal";
  private static final String STATUS_PARAM = "status";

  protected static final String SYNDICATION_ROOT = "/syndications";

  @Autowired
  private SyndicationCrudService syndicationService;


  @Transactional(readOnly = true)
  @RequestMapping(value = SYNDICATION_ROOT, method = RequestMethod.GET)
  public void getSyndications(
      @RequestParam(value = JOURNAL_PARAM, required = true) String journalKey,
      @RequestParam(value = STATUS_PARAM, required = true) List<String> statuses,
      HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    syndicationService.readSyndications(journalKey, statuses).respond(request, response, entityGson);
  }
}
