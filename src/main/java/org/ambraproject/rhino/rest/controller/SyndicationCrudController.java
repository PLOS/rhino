package org.ambraproject.rhino.rest.controller;

import org.ambraproject.rhino.model.Syndication;
import org.ambraproject.rhino.rest.controller.abstr.RestController;
import org.ambraproject.rhino.service.SyndicationCrudService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class SyndicationCrudController extends RestController {

  private static final Logger log = LoggerFactory.getLogger(SyndicationCrudController.class);

  private static final String JOURNAL_PARAM = "journal";
  private static final String STATUS_PARAM = "status";

  protected static final String SYNDICATION_ROOT = "/syndications";
  protected static final String SYNDICATION_NAMESPACE = SYNDICATION_ROOT + '/';
  protected static final String SYNDICATION_IN_PROGRESS_AND_FAILED = SYNDICATION_NAMESPACE + "/inProgressAndFailed";
  protected static final String SYNDICATION_BY_STATUS = SYNDICATION_NAMESPACE + "/{status}";

  @Autowired
  private SyndicationCrudService syndicationService;

  @Transactional(readOnly = true)
  @RequestMapping(value = SYNDICATION_IN_PROGRESS_AND_FAILED, method = RequestMethod.GET)
  public void getFailedAndInProgressSyndications(
      @RequestParam(value = JOURNAL_PARAM, required = true) String journalKey,
      HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    List<String> statuses = new ArrayList<>();
    statuses.add(Syndication.STATUS_FAILURE);
    statuses.add(Syndication.STATUS_IN_PROGRESS);
    syndicationService.readSyndications(journalKey, statuses).respond(request, response, entityGson);
  }

  @Transactional(readOnly = true)
  @RequestMapping(value = SYNDICATION_BY_STATUS, method = RequestMethod.GET)
  public void getSyndications(
      @RequestParam(value = JOURNAL_PARAM, required = true) String journalKey,
      @PathVariable(STATUS_PARAM) String status,
      HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    List<String> statuses = new ArrayList<>();
    statuses.add(status);
    syndicationService.readSyndications(journalKey, statuses).respond(request, response, entityGson);
  }
}
