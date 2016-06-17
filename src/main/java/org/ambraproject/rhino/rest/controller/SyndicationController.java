package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.SyndicationService;
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

@Controller
public class SyndicationController extends RestController {

  private static final Logger log = LoggerFactory.getLogger(SyndicationController.class);

  private static final String JOURNAL_PARAM = "journal";

  protected static final String SYNDICATION_ROOT = "/syndications";
  protected static final String SYNDICATION_NAMESPACE = SYNDICATION_ROOT + '/';
  protected static final String SYNDICATION_IN_PROGRESS_AND_FAILED = SYNDICATION_NAMESPACE + "/inProgressAndFailed";

  @Autowired
  private SyndicationService syndicationService;

  @Transactional(readOnly = true)
  @RequestMapping(value = SYNDICATION_IN_PROGRESS_AND_FAILED, method = RequestMethod.GET)
  public void getFailedAndInProgressSyndications(
      @RequestParam(value = JOURNAL_PARAM, required = true) String journalKey,
      HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    syndicationService.getFailedAndInProgressSyndications(journalKey); //todo: send through transceiver
  }
}
